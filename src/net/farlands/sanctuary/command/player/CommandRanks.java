package net.farlands.sanctuary.command.player;

import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.command.Command;
import net.farlands.sanctuary.command.CommandHandler;
import net.farlands.sanctuary.data.Rank;
import net.farlands.sanctuary.util.ReflectionHelper;
import net.farlands.sanctuary.util.TimeInterval;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class CommandRanks extends Command {
    public CommandRanks() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "Show all available player ranks.", "/ranks", "ranks");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(CommandSender sender, String[] args) {
        StringBuilder sb = new StringBuilder();

        for (Rank rank : Rank.VALUES) {
            // Skip the voter rank as it's only used internally
            if (Rank.VOTER.equals(rank))
                continue;

            // Don't show staff ranks
            if (rank.specialCompareTo(Rank.SPONSOR) > 0)
                break;

            sb.append(rank.getColor()).append(rank.getName()).append(ChatColor.BLUE);

            // For donor ranks, show the cost
            switch (rank) {
                case DONOR:
                case PATRON:
                case SPONSOR:
                    sb.append(" - ").append(Rank.DONOR_RANK_COSTS[rank.ordinal() - Rank.DONOR.ordinal()]).append(" USD");
                    break;

                default: {
                    int playTimeRequired = rank.getPlayTimeRequired();
                    // Ignore the initiate rank
                    if (playTimeRequired > 0)
                        sb.append(" - ").append(TimeInterval.formatTime(playTimeRequired * 60L * 60L * 1000L, false)).append(" play-time");
                }
            }

            // Show homes
            sb.append(" - ").append(rank.getHomes()).append(rank.getHomes() == 1 ? " home" : " homes");

            // Specify the new commands that come with the rank
            if (!Rank.INITIATE.equals(rank)) {
                List<String> cmds = ((List<Command>)ReflectionHelper.getFieldValue("commands", CommandHandler.class, FarLands.getCommandHandler()))
                        .stream().filter(cmd -> rank.equals(cmd.getMinRankRequirement()))
                        .map(cmd -> "/" + cmd.getName().toLowerCase())
                        .collect(Collectors.toList());

                if (!cmds.isEmpty())
                    sb.append(" -\n").append(String.join(", ", cmds));
            }

            sb.append('\n');
        }

        sender.sendMessage(sb.toString());
        return true;
    }
}
