package net.farlands.odyssey.command.staff;

import com.kicas.rp.util.Utils;

import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.command.Command;
import net.farlands.odyssey.data.struct.OfflineFLPlayer;
import net.farlands.odyssey.data.struct.Punishment;
import net.farlands.odyssey.data.Rank;
import net.farlands.odyssey.mechanic.Chat;
import net.farlands.odyssey.util.TimeInterval;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPunish extends Command {
    public CommandPunish() {
        super(Rank.JR_BUILDER, "Punish a player.", "/punish <player> <punishtype> [message]", true, "punish", "ban", "pardon");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < ("pardon".equals(args[0]) || "puniship".equals(args[0]) ? 2 : 3))
            return false;
        OfflineFLPlayer flp = FarLands.getDataHandler().getOfflineFLPlayerMatching(args[1]);
        if (flp == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        if (flp.uuid.equals(FarLands.getDataHandler().getOfflineFLPlayer(sender.getName()).getUuid())) {
            sender.sendMessage(ChatColor.RED + "That was close!");
            return true;
        }

        Rank senderRank = Rank.getRank(sender);
        if (!"pardon".equals(args[0])) {
            // Staff can mute players, Non-Jr. staff can mute Jr. staff, Owners can mute all staff except owners.
            if (!(sender instanceof ConsoleCommandSender) && ((senderRank.specialCompareTo(flp.rank) == 0) ||
                    ((senderRank.getPermissionLevel() == 2 || senderRank.getPermissionLevel() == 3) &&
                            (flp.rank.getPermissionLevel() == 2 || flp.rank.getPermissionLevel() == 3)) ||
                    (senderRank.getPermissionLevel() == 1 && flp.rank.getPermissionLevel() > 1))) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to punish this person.");
                return true;
            }
        }

        switch (args[0]) {
            case "punish":
            case "ban": {
                Punishment.PunishmentType pt = Utils.valueOfFormattedName(args[2], Punishment.PunishmentType.class);
                if (pt == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid punishment type: " + args[2]);
                    return true;
                }
                String punishMessage = args.length > 3 ? joinArgsBeyond(2, " ", args) : null;
                if (punishMessage != null && punishMessage.length() > 256) {
                    sender.sendMessage(ChatColor.RED + "Punishment messages are limited to 256 characters, it will be truncated.");
                    punishMessage = punishMessage.substring(0, 256);
                }
                punish(sender, flp, pt, punishMessage);
                break;
            }

            case "puniship": {
                FarLands.getDataHandler().getOfflineFLPlayers().stream()
                        .filter(flp0 -> flp.lastIP.equals(flp0.lastIP))
                        .filter(flp0 -> !flp0.getCurrentPunishment().getType().isPermanent())
                        .forEach(flp0 -> punish(sender, flp0, Punishment.PunishmentType.PERMANENT, null));
                break;
            }

            case "pardon": {
                Punishment.PunishmentType pt;
                if (args.length == 2) { // Remove latest punishment
                    Punishment punishment = flp.isBanned() ? flp.getCurrentPunishment() : flp.getMostRecentPunishment();
                    if (punishment == null) {
                        sender.sendMessage(ChatColor.RED + "This player has no punishments on record.");
                        return true;
                    }
                    pt = punishment.getType();
                } else { // Remove specific punishment
                    pt = Utils.valueOfFormattedName(args[2], Punishment.PunishmentType.class);
                    if (pt == null) {
                        sender.sendMessage(ChatColor.RED + "Invalid punishment type: " + args[2]);
                        return true;
                    }
                }
                if (flp.pardon(pt)) {
                    sender.sendMessage(ChatColor.GOLD + "Pardoned " + ChatColor.AQUA + flp.username +
                            ChatColor.GOLD + " from " + Utils.formattedName(pt));
                } else
                    sender.sendMessage(ChatColor.RED + "This player does not have that punishment on record.");
                break;
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        switch (args.length) {
            case 0:
            case 1:
                return getOnlinePlayers(args.length == 0 ? "" : args[0], sender);
            case 2:
                return Arrays.stream(Punishment.PunishmentType.VALUES).map(Utils::formattedName)
                        .filter(a -> a.startsWith(args[1])).collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }

    private void punish(CommandSender sender, OfflineFLPlayer flp, Punishment.PunishmentType pt, String punishMessage) {
        long time = flp.punish(pt, punishMessage);
        // The beginning 'P' is added later
        String message = "unished " + ChatColor.AQUA + flp.username + ChatColor.GOLD +
                " for " + Utils.formattedName(pt) + (punishMessage == null ? "" : " with message `" + punishMessage + "`") +
                ". Expires: " + (time < 0L ? "Never" : TimeInterval.formatTime(time, false, TimeInterval.MINUTE));
        sender.sendMessage(ChatColor.GOLD + "P" + message.replaceAll("`", "\""));
        FarLands.getDiscordHandler().sendMessageRaw("output", Chat.applyDiscordFilters(sender.getName()) +
                " has p" + Chat.removeColorCodes(message));
    }
}
