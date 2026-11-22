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

public class ItemLossSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public ItemLossSubcommand(ForgottenTempleMod plugin) {
        super("itemloss", "Toggle item loss on death (drop items)");
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
        String state = parseStateArgument(ctx, "itemloss");

        boolean currentEnabled = plugin.isItemLossEnabled();
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
            playerData.sendMessage(Message.raw("Usage: /ft itemloss [on/off]").color(YELLOW));
            return;
        }

        if (newEnabled == currentEnabled) {
            if (currentEnabled) {
                playerData.sendMessage(Message.raw("Item loss is already enabled (drop items on death).")
                        .color(YELLOW));
            } else {
                playerData.sendMessage(Message.raw("Item loss is already disabled (keep items on death).")
                        .color(YELLOW));
            }
            return;
        }

        plugin.setItemLoss(newEnabled);

        if (newEnabled) {
            playerData.sendMessage(Message.raw("Item loss enabled - players will drop items on death.")
                    .color(RED));
        } else {
            playerData.sendMessage(Message.raw("Item loss disabled - players will keep items on death.")
                    .color(GREEN));
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
