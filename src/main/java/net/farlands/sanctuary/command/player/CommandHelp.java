package net.farlands.sanctuary.command.player;

import com.kicas.rp.RegionProtection;
import com.kicas.rp.command.TabCompleterBase;
import com.kicas.rp.util.ReflectionHelper;
import com.kicas.rp.util.TextUtils2;
import com.kicasmads.cs.Utils;
import net.farlands.sanctuary.FarLands;
import net.farlands.sanctuary.command.Category;
import net.farlands.sanctuary.data.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandHelp extends net.farlands.sanctuary.command.Command {
    private static final int COMMANDS_PER_PAGE = 8;

    public CommandHelp() {
        super(Rank.INITIATE, Category.INFORMATIONAL, "View information on available commands.",
                "/help [category|command] [page]", "help");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean execute(CommandSender sender, String[] args) throws TextUtils2.ParserError {
        // Show list of categories
        if (args.length == 0) {
            TextUtils2.sendFormatted(
                sender,
                "&(gold)Commands are organized by category:\n" + Stream.of(Category.VALUES)
                    .filter(category -> category != Category.STAFF)
                    .map(category -> String.format(
                        "{$(click:run_command,%s)$(hover:show_text,%s)%s}",
                        "/help " + Utils.formattedName(category),
                        "{&(gray)Click to view this category}",
                        "&(gold)" + category.getAlias() + ": {&(white)" + category.getDescription() + "}"
                    ))
                    .collect(Collectors.joining("\n"))
            );
            return true;
        }

        Category category = Utils.valueOfFormattedName(args[0], Category.class);

        // Show info for a particular command
        if (category == null || category == Category.STAFF) {
            Command command = FarLands.getCommandHandler().getCommand(args[0]);
            if (command == null) {
                sender.sendMessage(ChatColor.RED + "Command or category not found: " + args[0]);
                return true;
            }

            TextUtils2.sendFormatted(
                    sender,
                    "&(gold)Showing info for command {&(aqua)%0}:\nUsage: %1\nDescription: {&(white)%2}",
                    command.getName(),
                    formatUsage(command.getUsage()),
                    command.getDescription()
            );
        } else {
            List<Command> commands;
            if (category == Category.CLAIMS) {
                Map<String, Command> knownCommands = (Map<String, org.bukkit.command.Command>) ReflectionHelper.getFieldValue(
                        "knownCommands",
                        SimpleCommandMap.class,
                        ((CraftServer) Bukkit.getServer()).getCommandMap()
                );
                Set<Command> commandSet = knownCommands.values().stream()
                        .filter(command -> command instanceof PluginCommand &&
                                ((PluginCommand)command).getPlugin() == RegionProtection.getInstance())
                        .collect(Collectors.toSet());
                commands = commandSet.stream()
                        .sorted(Comparator.comparing(Command::getUsage))
                        .collect(Collectors.toList());
            } else {
                commands = FarLands.getCommandHandler().getCommands().stream()
                        .filter(command -> command.getCategory().equals(category))
                        .sorted(Comparator.comparing(Command::getUsage))
                        .collect(Collectors.toList());
            }

            int page = 0;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "Invalid page number: " + args[1]);
                    return true;
                }
            }

            if (page < 0) {
                sender.sendMessage(ChatColor.RED + "Invalid page number: " + args[1]);
                return true;
            }

            int maxPageIndex = (commands.size() - 1) / COMMANDS_PER_PAGE;
            if (page > maxPageIndex) {
                TextUtils2.sendFormatted(sender, "&(red)This category only has %0 $(inflect,noun,0,page).", maxPageIndex + 1);
                return true;
            }

            TextUtils2.sendFormatted(
                    sender,
                    "&(gold)[%0] %1 - Page %2/%3 [%4]\n%5",
                    page == 0 ? "{&(gray)Prev}" : "{$(click:run_command,/help " + args[0] + " " + page + ")&(aqua)Prev})",
                    category.getAlias(),
                    page + 1,
                    maxPageIndex + 1,
                    page == maxPageIndex ? "{&(gray)Next}" : "{$(click:run_command,/help " + args[0] + " " + (page + 2) + ")&(aqua)Next}",
                    commands.stream().skip(page * COMMANDS_PER_PAGE).limit(COMMANDS_PER_PAGE)
                        .map(cmd -> String.format(
                            "{$(click:suggest_command,/%s)$(hover:show_text,%s)%s}",
                            cmd.getLabel(),
                            "{&(gold)" + formatUsage(cmd.getUsage()) + "}\n" +
                            "{&(gray)" + cmd.getDescription() + "}",
                            formatUsage(cmd.getUsage())
                        ))
                        .collect(Collectors.joining("\n"))
            );
        }

        return true;
    }

    public static String formatUsage(String usage) {
        return Arrays.stream(usage.split(" ")).map(arg -> {
            if (arg.startsWith("<") && arg.endsWith(">") || arg.startsWith("[") && arg.endsWith("]")) {
                String inner = arg.substring(1, arg.length() - 1).replaceAll("\\|", "{&(gold)|}").replaceAll("=", "{&(gold)=}");
                return arg.substring(0, 1) + "{&(white)" + inner + "}" + arg.substring(arg.length() - 1);
            } else
                return arg.replaceAll("\\|", "{&(gray)|}");
        }).collect(Collectors.joining(" "));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) throws IllegalArgumentException {
        return args.length <= 1
                ? TabCompleterBase.filterStartingWith(args[0], Arrays.stream(Category.values()).map(Category::name).map(String::toLowerCase))
                : Collections.emptyList();
    }
}
