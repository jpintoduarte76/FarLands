package net.farlands.odyssey.mechanic;

import com.kicas.rp.util.TextUtils;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.Cooldown;
import net.farlands.odyssey.data.FLPlayerSession;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.gui.GuiVillagerEditor;
import net.farlands.odyssey.util.Logging;
import net.farlands.odyssey.util.ReflectionHelper;
import net.farlands.odyssey.util.FLUtils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import net.minecraft.server.v1_15_R1.AdvancementDisplay;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.EntityVillager;
import net.minecraft.server.v1_15_R1.EntityVillagerAbstract;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.craftbukkit.v1_15_R1.advancement.CraftAdvancement;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftVillager;
import org.bukkit.entity.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GeneralMechanics extends Mechanic {
    private final Map<UUID, Player> fireworkLaunches;
    private BaseComponent[] joinMessage;

    private static final List<EntityType> LEASHABLE_ENTITIES = Arrays.asList(EntityType.SKELETON_HORSE,
            EntityType.VILLAGER, EntityType.TURTLE, EntityType.PANDA, EntityType.FOX);

    private Cooldown nightSkip;
    private List<UUID> leashedEntities;

    public GeneralMechanics() {
        this.fireworkLaunches = new HashMap<>();
        this.joinMessage = new BaseComponent[0];
        this.nightSkip = new Cooldown(200L);
        leashedEntities = new ArrayList<>();
    }

    @Override
    public void onStartup() {
        try {
            joinMessage = TextUtils.format(FarLands.getDataHandler().getDataTextFile("join-message.txt"), FarLands.getFLConfig().discordInvite);
        } catch (IOException ex) {
            Logging.error("Failed to load join message!");
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), () ->
                Bukkit.getOnlinePlayers().forEach(player -> {
                    OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
                    if (flp.hasParticles() && !flp.isVanished() && !GameMode.SPECTATOR.equals(player.getGameMode()))
                        flp.getParticles().spawn(player);
                }), 0L, 60L);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(FarLands.getInstance(), () -> {
            Bukkit.getWorld("world").getEntities().stream().filter(e -> EntityType.DROPPED_ITEM.equals(e.getType()))
                    .map(e -> (Item) e).filter(e -> Material.SLIME_BALL.equals(e.getItemStack().getType()) && e.isValid() && e.getLocation().getChunk().isSlimeChunk())
                    .forEach(e -> {
                        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);
                        e.setVelocity(new org.bukkit.util.Vector(0.0, 0.4, 0.0));
                    });
        }, 0L, 100L);
    }

    @Override
    public void onPlayerJoin(Player player, boolean isNew) {
        player.spigot().sendMessage(joinMessage);

        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () ->
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.6929134F), 45L);
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () ->
                player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.85F, 1.480315F), 95L);
        Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.5F);
            OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(player);
            if (!flp.viewedPatchnotes())
                player.sendMessage(ChatColor.GOLD + "Patch " + ChatColor.AQUA + "#" + FarLands.getDataHandler().getCurrentPatch() +
                        ChatColor.GOLD + " has been released! View changes with " + ChatColor.AQUA + "/patchnotes");
        }, 125L);

        if (isNew) {
            Logging.broadcast(p -> {
                Player pl = p.handle.getOnlinePlayer();
                if (!player.getUniqueId().equals(p.handle.uuid)) {
                    if (pl != null)
                        pl.playSound(pl.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5.0F, 1.0F);
                    return true;
                } else
                    return false;
            }, "&(gold){&(bold)>} Welcome {&(green)%0} to FarLands!", player.getName());
            player.chat("/chain {guidebook} {shovel}");
            TextUtils.sendFormatted(player, "&(gold)Welcome to FarLands! Please read $(hovercmd,/rules,&(aqua)Click " +
                    "to view the server rules.,&(aqua)our rules) before playing. To get started, you can use " +
                    "$(hovercmd,/wild,&(aqua)Click to go to a random location.,&(aqua)/wild) to teleport to a " +
                    "random location on the map. Also, feel free to join our community on discord by clicking " +
                    "$(link,%0,&(aqua)here.)", FarLands.getFLConfig().discordInvite);
        }

        if ("world".equals(player.getWorld().getName()))
            updateNightSkip(true, 0, 0);
    }

    @Override
    public void onPlayerQuit(Player player) {
        if (player.getVehicle() != null && FarLands.getDataHandler().getSession(player).seatExit != null) {
            player.getVehicle().remove();
            player.teleport(FarLands.getDataHandler().getSession(player).seatExit);
            FarLands.getDataHandler().getSession(player).seatExit = null;
        }
        updateNightSkip(true, 1, 0);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();
        for (int i = 0; i < lines.length; ++i)
            event.setLine(i, Chat.applyColorCodes(Rank.getRank(event.getPlayer()), lines[i]));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (Material.DRAGON_EGG.equals(event.getClickedBlock() == null ? null : event.getClickedBlock().getType()) && !event.isCancelled()) {
            event.setCancelled(true);
            event.getClickedBlock().setType(Material.AIR);
            FLUtils.giveItem(event.getPlayer(), new ItemStack(Material.DRAGON_EGG), false);
            event.getPlayer().playSound(event.getClickedBlock().getLocation(), Sound.ENTITY_ITEM_PICKUP, 6.0F, 1.0F);
            return;
        }
        ItemStack chestplate = player.getInventory().getChestplate();
        if (Material.FIREWORK_ROCKET.equals(event.getMaterial()) && EquipmentSlot.HAND.equals(event.getHand()) &&
                Action.RIGHT_CLICK_BLOCK.equals(event.getAction()) && Material.ELYTRA.equals(chestplate == null ? null :
                chestplate.getType()) && !player.isGliding()) {
            event.setCancelled(true);
            if (!GameMode.CREATIVE.equals(player.getGameMode())) {
                PlayerInventory inv = player.getInventory();
                ItemStack hand = inv.getItemInMainHand();
                if (hand.getAmount() == 1)
                    inv.setItemInMainHand(null);
                else
                    hand.setAmount(hand.getAmount() - 1);
            }
            Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
            firework.addPassenger(player);
            fireworkLaunches.put(firework.getUniqueId(), player);
            // Add if another condition is added below
            //return;
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity ent = event.getRightClicked();
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (EntityType.VILLAGER.equals(event.getRightClicked().getType()) && GameMode.CREATIVE.equals(event.getPlayer().getGameMode()) &&
                Material.BLAZE_ROD.equals(hand.getType()) && Rank.getRank(event.getPlayer()).isStaff()) {
            event.setCancelled(true);
            FarLands.getDataHandler().getPluginData().addSpawnTrader(ent.getUniqueId());
            (new GuiVillagerEditor((CraftVillager) ent)).openGui(event.getPlayer());
        } else if (LEASHABLE_ENTITIES.contains(event.getRightClicked().getType()) && !FLUtils.isInSpawn(event.getRightClicked().getLocation()) &&
                event.getRightClicked() instanceof LivingEntity && hand != null) {
            final LivingEntity entity = (LivingEntity) ent;
            if (Material.LEAD.equals(hand.getType()) && ent instanceof LivingEntity) {
                if (entity.isLeashed())
                    return;
                event.setCancelled(true); // Don't open any GUIs

                // Prevent double lead usage with nitwits
                if (leashedEntities.contains(entity.getUniqueId()))
                    return;
                leashedEntities.add(entity.getUniqueId());
                FarLands.getScheduler().scheduleSyncDelayedTask(() -> leashedEntities.remove(entity.getUniqueId()), 5L);


                if (hand.getAmount() > 1)
                    hand.setAmount(hand.getAmount() - 1);
                else {
                    hand.setAmount(0);
                    hand.setType(Material.AIR);
                }
                Bukkit.getScheduler().runTask(FarLands.getInstance(), () -> entity.setLeashHolder(event.getPlayer()));
            } else if (entity.isLeashed()) {
                event.setCancelled(true);
                entity.setLeashHolder(null);
                Item item = (Item) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.DROPPED_ITEM);
                item.setItemStack(new ItemStack(Material.LEAD));
            }
        } else if (FarLands.getDataHandler().getPluginData().isSpawnTrader(event.getRightClicked().getUniqueId())) {
            event.setCancelled(true);
            EntityVillager handle = ((CraftVillager) event.getRightClicked()).getHandle(), duplicate = new EntityVillager(EntityTypes.VILLAGER, handle.world);
            duplicate.setPosition(0.0, 0.0, 0.0);
            duplicate.setCustomName(handle.getCustomName());
            duplicate.setVillagerData(handle.getVillagerData());
            ReflectionHelper.setFieldValue("trades", EntityVillagerAbstract.class, duplicate, FLUtils.copyRecipeList(handle.getOffers()));
            event.getPlayer().openMerchant(new CraftVillager((CraftServer) Bukkit.getServer(), duplicate), true);
        } else if (ent instanceof Tameable) {
            Tameable pet = (Tameable) ent;
            if (!(pet.isTamed() && pet.getOwner() != null && (event.getPlayer().getUniqueId().equals(pet.getOwner().getUniqueId()) ||
                    Rank.getRank(event.getPlayer()).isStaff())))
                return;

            Player petRecipient = FarLands.getDataHandler().getSession(event.getPlayer()).givePetRecipient.getValue();
            FarLands.getDataHandler().getSession(event.getPlayer()).givePetRecipient = null;
            if (petRecipient == null)
                return;
            if (FarLands.getDataHandler().getOfflineFLPlayer(petRecipient).isIgnoring(event.getPlayer()))
                pet.remove(); // fake the pet teleporting

            pet.setOwner(petRecipient);
            event.getPlayer().sendMessage("Successfully transferred pet to " + petRecipient.getName());
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdvancementCompleted(PlayerAdvancementDoneEvent event) {
        FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer()).updateSessionIfOnline(true);
        AdvancementDisplay ad = ((CraftAdvancement) event.getAdvancement()).getHandle().c();
        if (ad != null && !FarLands.getDataHandler().getOfflineFLPlayer(event.getPlayer()).vanished) {
            Logging.broadcastIngame(TextComponent.fromLegacyText(event.getPlayer().getDisplayName() + ChatColor.RESET +
                    " has made the advancement " + ChatColor.GREEN + "[" + ad.a().getText() + "]"));
            FarLands.getDiscordHandler().sendMessage("ingame", event.getPlayer().getDisplayName() +
                    " has made the advancement [" + ad.a().getText() + "]");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) { // Allow teleporting with leashed entities
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            event.getPlayer().getNearbyEntities(10.0, 10.0, 10.0).stream().filter(e -> e instanceof LivingEntity)
                    .map(e -> (LivingEntity) e).filter(e -> e.isLeashed() && event.getPlayer().equals(e.getLeashHolder())).forEach(e -> {
                e.setLeashHolder(null);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    e.getLocation().getChunk().load();
                    Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                        e.teleport(event.getTo());
                        e.setLeashHolder(event.getPlayer());
                    }, 1);
                }, 1);
            });
        } else
            updateNightSkip(false, 1, 0);
    }

    @EventHandler
    public void onFireworkExplode(FireworkExplodeEvent event) {
        if (fireworkLaunches.containsKey(event.getEntity().getUniqueId())) {
            Player player = fireworkLaunches.get(event.getEntity().getUniqueId());
            if (player.isValid() && !"farlands".equals(player.getWorld().getName())) {
                player.setGliding(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        updateNightSkip(true, 0, 1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player))
            return;
        FLPlayerSession session = FarLands.getDataHandler().getSession((Player) event.getExited());
        if (session.seatExit != null) {
            event.setCancelled(true);
            event.getVehicle().eject();
            event.getVehicle().remove();
            event.getExited().teleport(session.seatExit);
            session.seatExit = null;
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        switch (event.getEntityType()) {
            case ENDER_DRAGON:
                event.setDroppedExp(4000);
                Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                    Block block = event.getEntity().getWorld().getBlockAt(0, 75, 0);
                    block.setType(Material.DRAGON_EGG);
                    block.getWorld().getNearbyEntities(block.getLocation(), 50, 50, 50)
                            .forEach(e -> e.sendMessage(ChatColor.GRAY + "As the dragon dies, an egg forms below."));
                }, 15L * 20L);
                break;
            case VILLAGER:
                FarLands.getDataHandler().getPluginData().removeSpawnTrader(event.getEntity().getUniqueId());
                break;
        }
    }

    private void updateNightSkip(boolean sendBroadcast, int roff, int soff) {
        int dayTime = (int) (Bukkit.getWorld("world").getTime() % 24000);
        if (12541 > dayTime || dayTime > 23458)
            return;

        List<Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(player -> "world".equals(player.getWorld().getName())).map(player -> (Player) player)
                .filter(player -> !FarLands.getDataHandler().getOfflineFLPlayer(player).vanished)
                .collect(Collectors.toList());
        int sleeping = (int) online.stream().filter(Player::isSleeping).count() + soff;
        if (sleeping <= 0)
            return;

        int required = (online.size() + 1 - roff) / 2;
        if (sleeping < required) {
            if (sendBroadcast && nightSkip.isComplete()) {
                nightSkip.reset();
                Logging.broadcastFormatted("%0 &(gold)more $(inflect,noun,0,player) $(inflect,verb,0,need) " +
                        "to sleep to skip the night.", false, required - sleeping);
            }
        } else if (required == sleeping) {
            Logging.broadcastFormatted("&(gold)Skipping the night...", false);
            Bukkit.getScheduler().runTaskLater(FarLands.getInstance(), () -> {
                World world = Bukkit.getWorld("world");
                world.setTime(1000L);
                world.setStorm(false);
            }, 30L);
        }
    }
}
