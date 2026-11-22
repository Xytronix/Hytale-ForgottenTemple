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

public class PvPSpawnSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public PvPSpawnSubcommand(ForgottenTempleMod plugin) {
        super("pvpspawn", "Toggle PvP protection at spawn area");
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
        String state = parseStateArgument(ctx, "pvpspawn");

        boolean currentProtected = plugin.isPvPAtSpawnDisabled();
        boolean newProtected;

        if (state == null) {
            newProtected = !currentProtected;
        } else if (state.equalsIgnoreCase("on")
                || state.equalsIgnoreCase("enable")
                || state.equalsIgnoreCase("true")
                || state.equalsIgnoreCase("protect")) {
            newProtected = true;
        } else if (state.equalsIgnoreCase("off")
                || state.equalsIgnoreCase("disable")
                || state.equalsIgnoreCase("false")
                || state.equalsIgnoreCase("allow")) {
            newProtected = false;
        } else {
            playerData.sendMessage(Message.raw("Usage: /ft pvpspawn [on/off]").color(YELLOW));
            playerData.sendMessage(
                    Message.raw("  on = PvP blocked at spawn area").color(YELLOW));
            playerData.sendMessage(
                    Message.raw("  off = PvP allowed at spawn area").color(YELLOW));
            return;
        }

        if (newProtected == currentProtected) {
            if (currentProtected) {
                playerData.sendMessage(Message.raw("PvP at spawn is already protected (blocked).")
                        .color(YELLOW));
            } else {
                playerData.sendMessage(
                        Message.raw("PvP at spawn is already allowed.").color(YELLOW));
            }
            return;
        }

        plugin.setDisablePvPAtSpawn(newProtected);

        if (newProtected) {
            playerData.sendMessage(Message.raw("PvP at spawn area is now protected - players cannot fight at spawn.")
                    .color(GREEN));
        } else {
            playerData.sendMessage(Message.raw("PvP at spawn area is now allowed - players can fight at spawn.")
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
