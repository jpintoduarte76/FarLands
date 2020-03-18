package net.farlands.odyssey.util;

import com.kicas.rp.util.TextUtils;
import net.farlands.odyssey.FarLands;
import net.farlands.odyssey.data.FLPlayerSession;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Predicate;

public final class Logging {
    public static void broadcastIngame(BaseComponent[] message) {
        Bukkit.getOnlinePlayers().stream().map(Player::spigot).forEach(spigot -> spigot.sendMessage(message));
        Bukkit.getConsoleSender().spigot().sendMessage(message);
    }

    public static void broadcastFormatted(String message, boolean sendToDiscord, Object... values) {
        BaseComponent[] formatted = TextUtils.format("{&(gold,bold) > }&(aqua)" + message, values);
        broadcastIngame(formatted);
        if (sendToDiscord)
            FarLands.getDiscordHandler().sendMessage("ingame", formatted);
    }

    public static void broadcast(String input, Object... values) {
        BaseComponent[] formatted = TextUtils.format(input, values);
        broadcastIngame(formatted);
        FarLands.getDiscordHandler().sendMessage("ingame", formatted);
    }

    public static void broadcast(Predicate<FLPlayerSession> filter, String input, Object... values) {
        BaseComponent[] formatted = TextUtils.format(input, values);
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession).filter(filter)
                .forEach(session -> session.player.spigot().sendMessage(formatted));
        Bukkit.getConsoleSender().spigot().sendMessage(formatted);
        FarLands.getDiscordHandler().sendMessage("ingame", formatted);
    }

    public static void broadcastStaff(BaseComponent[] message, String discordChannel) { // Set the channel to null to not send to discord
        Bukkit.getOnlinePlayers().stream().map(FarLands.getDataHandler()::getSession)
                .filter(session -> session.handle.rank.isStaff() && session.showStaffChat)
                .forEach(session -> session.player.spigot().sendMessage(message));
        Bukkit.getConsoleSender().spigot().sendMessage(message);
        if (discordChannel != null)
            FarLands.getDiscordHandler().sendMessage(discordChannel, message);
    }

    public static void broadcastStaff(BaseComponent[] message) {
        broadcastStaff(message, null);
    }

    public static void broadcastStaff(String message, String discordChannel) {
        broadcastStaff(TextComponent.fromLegacyText(message), discordChannel);
    }

    public static void broadcastStaff(String message) {
        broadcastStaff(TextComponent.fromLegacyText(message), null);
    }

    public static void log(Object x) {
        Bukkit.getLogger().info("[FLv2] - " + x);
    }

    public static void error(Object x) {
        String msg = Objects.toString(x);
        Bukkit.getLogger().severe("[FLv2] - " + msg);
        FarLands.getDebugger().echo("Error", msg);
        FarLands.getDiscordHandler().sendMessageRaw("output", msg);
    }
}