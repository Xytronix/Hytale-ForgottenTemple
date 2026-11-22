package com.xytronix.forgottentemple.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.xytronix.forgottentemple.ForgottenTempleMod;
import java.awt.Color;
import javax.annotation.Nonnull;

public class TimeSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color AQUA = new Color(85, 255, 255);

    public TimeSubcommand(ForgottenTempleMod plugin) {
        super("time", "Set frozen time (day/night/HH:MM/none)");
        this.plugin = plugin;
        setAllowsExtraArguments(true);
        requirePermission("forgottentemple.admin");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull PlayerRef playerData,
            @Nonnull World world) {
        String timeArg = parseStateArgument(ctx, "time");

        if (timeArg == null || timeArg.isEmpty()) {
            String current = plugin.getFreezeTime();
            if (current == null || current.isEmpty()) {
                playerData.sendMessage(
                        Message.raw("Time is not frozen (dynamic).").color(AQUA));
            } else {
                playerData.sendMessage(Message.raw("Frozen time: " + current).color(AQUA));
            }
            playerData.sendMessage(
                    Message.raw("Usage: /ft time [day/night/HH:MM/none]").color(YELLOW));
            return;
        }

        String normalizedTime = normalizeTime(timeArg);

        if (normalizedTime == null) {
            playerData.sendMessage(
                    Message.raw("Invalid time format: " + timeArg).color(RED));
            playerData.sendMessage(Message.raw("Valid options: day, night, HH:MM (e.g. 14:30), none")
                    .color(YELLOW));
            return;
        }

        if (normalizedTime.isEmpty()) {
            plugin.setFreezeTime(null);
            playerData.sendMessage(
                    Message.raw("Time unfrozen - time is now dynamic.").color(GREEN));
        } else {
            plugin.setFreezeTime(normalizedTime);
            String displayTime = getDisplayTime(normalizedTime);
            playerData.sendMessage(Message.raw("Time frozen to: " + displayTime).color(GREEN));
        }
    }

    private String normalizeTime(String input) {
        if (input == null) {
            return null;
        }
        String lower = input.toLowerCase();

        switch (lower) {
            case "day", "noon", "midday" -> {
                return "day";
            }
            case "night", "midnight" -> {
                return "night";
            }
            case "morning", "dawn", "sunrise" -> {
                return "6:00";
            }
            case "evening", "dusk", "sunset" -> {
                return "18:00";
            }
            case "none", "off", "disable", "dynamic", "unfreeze" -> {
                return "";
            }
        }

        if (input.matches("\\d{1,2}:\\d{2}")) {
            String[] parts = input.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60) {
                return input;
            }
        }

        return null;
    }

    private String getDisplayTime(String time) {
        if ("day".equalsIgnoreCase(time)) {
            return "day (12:00)";
        } else if ("night".equalsIgnoreCase(time)) {
            return "night (03:00)";
        }
        return time;
    }

    private String parseStateArgument(CommandContext ctx, String commandName) {
        String input = ctx.getInputString();
        int cmdIndex = input.toLowerCase().lastIndexOf(commandName);
        if (cmdIndex >= 0) {
            String afterCmd = input.substring(cmdIndex + commandName.length()).trim();
            if (!afterCmd.isEmpty()) {
                return afterCmd.split("\\s+")[0];
            }
        }
        return null;
    }
}
