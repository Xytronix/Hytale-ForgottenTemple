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

public class BuildSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public BuildSubcommand(ForgottenTempleMod plugin) {
        super("build", "Toggle building (block break/place) in the temple");
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
        String state = parseStateArgument(ctx, "build");

        boolean currentProtectWorld = plugin.isProtectWorld();
        boolean newProtectWorld;

        if (state == null) {
            newProtectWorld = !currentProtectWorld;
        } else if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("enable") || state.equalsIgnoreCase("true")) {
            newProtectWorld = false;
        } else if (state.equalsIgnoreCase("off")
                || state.equalsIgnoreCase("disable")
                || state.equalsIgnoreCase("false")) {
            newProtectWorld = true;
        } else {
            playerData.sendMessage(Message.raw("Usage: /ft build [on/off]").color(YELLOW));
            return;
        }

        if (newProtectWorld == currentProtectWorld) {
            if (currentProtectWorld) {
                playerData.sendMessage(Message.raw("Building is already disabled (world protected).")
                        .color(YELLOW));
            } else {
                playerData.sendMessage(
                        Message.raw("Building is already enabled.").color(YELLOW));
            }
            return;
        }

        plugin.setProtectWorld(newProtectWorld);

        if (newProtectWorld) {
            playerData.sendMessage(Message.raw("Building has been disabled - world protection is now ON.")
                    .color(GREEN));
        } else {
            playerData.sendMessage(Message.raw("Building has been enabled - world protection is now OFF.")
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
