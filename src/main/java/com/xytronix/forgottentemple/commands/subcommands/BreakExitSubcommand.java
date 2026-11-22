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

public class BreakExitSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public BreakExitSubcommand(ForgottenTempleMod plugin) {
        super("breakexit", "Allow exit portal breaking (all worlds)");
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
        String state = parseStateArgument(ctx, "breakexit");

        boolean currentAllowed = plugin.isAllowBreakExitPortal();
        boolean newAllowed;

        if (state == null) {
            newAllowed = !currentAllowed;
        } else if (state.equalsIgnoreCase("on")
                || state.equalsIgnoreCase("enable")
                || state.equalsIgnoreCase("true")
                || state.equalsIgnoreCase("allow")) {
            newAllowed = true;
        } else if (state.equalsIgnoreCase("off")
                || state.equalsIgnoreCase("disable")
                || state.equalsIgnoreCase("false")
                || state.equalsIgnoreCase("deny")) {
            newAllowed = false;
        } else {
            playerData.sendMessage(Message.raw("Usage: /ft breakexit [on/off]").color(YELLOW));
            return;
        }

        if (newAllowed == currentAllowed) {
            playerData.sendMessage(
                    Message.raw("Exit portal breaking is already " + (currentAllowed ? "allowed" : "denied") + ".")
                            .color(YELLOW));
            return;
        }

        plugin.setAllowBreakExitPortal(newAllowed);

        if (newAllowed) {
            playerData.sendMessage(Message.raw("Exit portal blocks can now be broken (all worlds).")
                    .color(GREEN));
        } else {
            playerData.sendMessage(Message.raw("Exit portal blocks are now protected (all worlds).")
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
