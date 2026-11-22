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

public class FallDamageSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public FallDamageSubcommand(ForgottenTempleMod plugin) {
        super("falldamage", "Toggle fall damage in the temple world");
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
        String state = parseStateArgument(ctx, "falldamage");

        boolean currentDisabled = plugin.isFallDamageDisabled();
        boolean newDisabled;

        if (state == null) {
            newDisabled = !currentDisabled;
        } else if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("enable") || state.equalsIgnoreCase("true")) {
            newDisabled = false;
        } else if (state.equalsIgnoreCase("off")
                || state.equalsIgnoreCase("disable")
                || state.equalsIgnoreCase("false")) {
            newDisabled = true;
        } else {
            playerData.sendMessage(Message.raw("Usage: /ft falldamage [on/off]").color(YELLOW));
            return;
        }

        if (newDisabled == currentDisabled) {
            if (currentDisabled) {
                playerData.sendMessage(
                        Message.raw("Fall damage is already disabled.").color(YELLOW));
            } else {
                playerData.sendMessage(
                        Message.raw("Fall damage is already enabled.").color(YELLOW));
            }
            return;
        }

        plugin.setDisableFallDamage(newDisabled);

        if (newDisabled) {
            playerData.sendMessage(
                    Message.raw("Fall damage has been disabled in the temple.").color(GREEN));
        } else {
            playerData.sendMessage(
                    Message.raw("Fall damage has been enabled in the temple.").color(RED));
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
