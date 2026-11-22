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

public class PvpSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public PvpSubcommand(ForgottenTempleMod plugin) {
        super("pvp", "Toggle PvP in the temple world");
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
        String state = parseStateArgument(ctx, "pvp");

        boolean currentState = plugin.isPvPDisabled();
        boolean newDisableState;

        if (state == null) {
            newDisableState = !currentState;
        } else if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("enable") || state.equalsIgnoreCase("true")) {
            newDisableState = false;
        } else if (state.equalsIgnoreCase("off")
                || state.equalsIgnoreCase("disable")
                || state.equalsIgnoreCase("false")) {
            newDisableState = true;
        } else {
            playerData.sendMessage(Message.raw("Usage: /ft pvp [on/off]").color(YELLOW));
            return;
        }

        if (newDisableState == currentState) {
            if (currentState) {
                playerData.sendMessage(
                        Message.raw("PvP is already disabled in the temple.").color(YELLOW));
            } else {
                playerData.sendMessage(
                        Message.raw("PvP is already enabled in the temple.").color(YELLOW));
            }
            return;
        }

        plugin.setDisablePvP(newDisableState);

        if (newDisableState) {
            playerData.sendMessage(Message.raw("PvP has been disabled in the Forgotten Temple.")
                    .color(GREEN));
        } else {
            playerData.sendMessage(
                    Message.raw("PvP has been enabled in the Forgotten Temple.").color(RED));
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
