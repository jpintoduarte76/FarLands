package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.data.FlagContainer;
import com.kicas.rp.data.RegionFlag;
import com.kicas.rp.data.flagdata.TrustLevel;
import com.kicas.rp.data.flagdata.TrustMeta;
import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.data.Shop;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.PlayerCommand;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.data.struct.OfflineFLPlayer;
import net.farlands.sanctuary.util.ComponentColor;
import net.farlands.sanctuary.util.FLUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandEditSign extends PlayerCommand {

    public CommandEditSign() {
        super(Rank.SAGE, Category.UTILITY, "Edit a specific line of a sign.", "/editsign <set/clear> <line> <text>", "editsign");
    }

    @Override
    protected boolean execute(Player sender, String[] args) {

        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayer(sender);

        // Make sure the player has build permission in the region they're in
        FlagContainer flags = RegionProtection.getDataManager().getFlagsAt(sender.getLocation());
        if (flags != null && !flags.<TrustMeta>getFlagMeta(RegionFlag.TRUST).hasTrust(sender, TrustLevel.BUILD, flags)) {
            sender.sendMessage(ComponentColor.red("You do not have permissions to edit signs here."));
            return true;
        }

        Block target = sender.getTargetBlock(null, 5); // 5 seems like a good number

        // Make sure the sign is not a chest shop
        Shop shop = ChestShops.getDataHandler().getShop(target.getLocation());
        if (shop != null) {
            sender.sendMessage(ComponentColor.red("You cannot edit chest shop signs."));
            return true;
        }

        // Make sure the player is looking at a sign
        if (!(target.getState() instanceof Sign)) {
            sender.sendMessage(ComponentColor.red("You must be looking at a sign to use this command."));
            return true;
        }

        Sign sign = (Sign) target.getState();
        try {
            if (args[0].equalsIgnoreCase("set") && args.length > 2) {
                int line = Integer.parseInt(args[1]) -1;
                String text = TabCompleterBase.joinArgsBeyond(1, " ", args);
                if (FLUtils.removeColorCodes(text).length() > 15) {
                    sender.sendMessage(ComponentColor.red("Sign lines are limited to 15 characters."));
                    return true;
                }
                sign.setLine(line, FLUtils.applyColorCodes(flp.rank, text));
                sign.update();
                sender.sendMessage(ComponentColor.gold("Line " + (line+1) + " set to: " + FLUtils.applyColorCodes(flp.rank, text)));
            } else if (args[0].equalsIgnoreCase("clear")) {
                if (args.length == 1) {
                    for (int i = 0; i < 4; i++) {
                        sign.setLine(i, "");
                    }
                    sign.update();
                    sender.sendMessage(ComponentColor.gold("Sign text cleared."));
                } else {
                    int line = Integer.parseInt(args[1]) - 1;
                    sign.setLine(line, "");
                    sign.update();
                    sender.sendMessage(ComponentColor.gold("Line " + (line+1) + " text cleared."));
                }
            } else {
                return false;
            }
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        if (args.length == 1) {
            return Arrays.asList("set", "clear");
        } else if (args.length == 2) {
            return Arrays.asList("1", "2", "3", "4");
        } else if (args.length >= 3) {
            if (FLUtils.removeColorCodes(TabCompleterBase.joinArgsBeyond(1, " ", args)).length() > 15) {
                return Collections.singletonList("Too Long!");
            }
        } else {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
