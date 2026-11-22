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

public class GodModeSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public GodModeSubcommand(ForgottenTempleMod plugin) {
        super("godmode", "Toggle player invulnerability");
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
        String state = parseStateArgument(ctx, "godmode");

        boolean currentEnabled = plugin.isGodModeEnabled();
        boolean newEnabled;

        if (state == null) {
            newEnabled = !currentEnabled;
        } else if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("enable") || state.equalsIgnoreCase("true")) {
            newEnabled = true;
        } else if (state.equalsIgnoreCase("off")
                || state.equalsIgnoreCase("disable")
                || state.equalsIgnoreCase("false")) {
            newEnabled = false;
        } else {
            playerData.sendMessage(Message.raw("Usage: /ft godmode [on/off]").color(YELLOW));
            return;
        }

        if (newEnabled == currentEnabled) {
            if (currentEnabled) {
                playerData.sendMessage(Message.raw("God mode is already enabled (players invulnerable).")
                        .color(YELLOW));
            } else {
                playerData.sendMessage(Message.raw("God mode is already disabled (players take damage).")
                        .color(YELLOW));
            }
            return;
        }

        plugin.setGodMode(newEnabled);

        if (newEnabled) {
            playerData.sendMessage(Message.raw("God mode enabled - players are now invulnerable.")
                    .color(GREEN));
        } else {
            playerData.sendMessage(Message.raw("God mode disabled - players can now take damage.")
                    .color(RED));
        }
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
