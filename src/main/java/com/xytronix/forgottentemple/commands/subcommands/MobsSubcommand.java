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

public class MobsSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public MobsSubcommand(ForgottenTempleMod plugin) {
        super("mobs", "Toggle mob spawning in the temple world");
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
        String state = parseStateArgument(ctx, "mobs");

        boolean currentDisabled = plugin.isMobsDisabled();
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
            playerData.sendMessage(Message.raw("Usage: /ft mobs [on/off]").color(YELLOW));
            return;
        }

        if (newDisabled == currentDisabled) {
            if (currentDisabled) {
                playerData.sendMessage(
                        Message.raw("Mob spawning is already disabled.").color(YELLOW));
            } else {
                playerData.sendMessage(
                        Message.raw("Mob spawning is already enabled.").color(YELLOW));
            }
            return;
        }

        plugin.setDisableMobs(newDisabled);

        if (newDisabled) {
            playerData.sendMessage(
                    Message.raw("Mob spawning has been disabled in the temple.").color(GREEN));
        } else {
            playerData.sendMessage(
                    Message.raw("Mob spawning has been enabled in the temple.").color(RED));
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
