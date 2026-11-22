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

public class FreezeNPCsSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public FreezeNPCsSubcommand(ForgottenTempleMod plugin) {
        super("freezenpcs", "Toggle NPC freeze (stops all NPC movement)");
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
        String state = parseStateArgument(ctx, "freezenpcs");

        boolean currentFrozen = plugin.isNPCsFrozen();
        boolean newFrozen;

        if (state == null) {
            newFrozen = !currentFrozen;
        } else if (state.equalsIgnoreCase("on")
                || state.equalsIgnoreCase("enable")
                || state.equalsIgnoreCase("true")
                || state.equalsIgnoreCase("freeze")) {
            newFrozen = true;
        } else if (state.equalsIgnoreCase("off")
                || state.equalsIgnoreCase("disable")
                || state.equalsIgnoreCase("false")
                || state.equalsIgnoreCase("unfreeze")) {
            newFrozen = false;
        } else {
            playerData.sendMessage(Message.raw("Usage: /ft freezenpcs [on/off]").color(YELLOW));
            return;
        }

        if (newFrozen == currentFrozen) {
            if (currentFrozen) {
                playerData.sendMessage(Message.raw("NPCs are already frozen.").color(YELLOW));
            } else {
                playerData.sendMessage(Message.raw("NPCs are already unfrozen.").color(YELLOW));
            }
            return;
        }

        plugin.setFreezeNPCs(newFrozen);

        if (newFrozen) {
            playerData.sendMessage(Message.raw("NPCs are now frozen - all NPC movement stopped.")
                    .color(GREEN));
        } else {
            playerData.sendMessage(
                    Message.raw("NPCs are now unfrozen - NPCs can move again.").color(RED));
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
