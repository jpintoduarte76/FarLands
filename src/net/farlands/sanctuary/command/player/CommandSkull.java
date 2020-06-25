package net.farlands.sanctuary.command.player;

import static com.kicas.rp.util.TextUtils.sendFormatted;

import com.kicas.rp.RegionProtection;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.FLPlayerSession;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.TimeInterval;
import net.farlands.sanctuary.util.FLUtils;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CommandSkull extends PlayerCommand {
    public CommandSkull() {
        super(Rank.SAGE, Category.COSMETIC, "Give yourself a player's head.", "/skull <name> [amount]", "skull");
    }

    @Override
    public boolean execute(Player sender, String[] args) {
        if (args.length == 0)
            return false;

        FLPlayerSession session = FarLands.getDataHandler().getSession(sender);
        long cooldownTime = session.commandCooldownTimeRemaining(this);
        if (cooldownTime > 0L) {
            sendFormatted(sender, "&(red)You can use this command again in %0.",
                    TimeInterval.formatTime(cooldownTime * 50L, false));
            return true;
        }
        session.setCommandCooldown(this, 400L);

        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sendFormatted(sender, "&(red)Invalid amount.");
                return true;
            }

            if (amount < 1)
                amount = 1;
        }

        UUID ownerId = RegionProtection.getDataManager().uuidForUsername(args[0]);
        if (ownerId == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        OfflinePlayer player = Bukkit.getOfflinePlayer(ownerId);
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.RESET + player.getName());
        skull.setItemMeta(meta);
        FLUtils.giveItem(sender, skull, true);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1 ? getOnlinePlayers(args.length == 0 ? "" : args[0], sender) : Collections.emptyList();
    }
}
