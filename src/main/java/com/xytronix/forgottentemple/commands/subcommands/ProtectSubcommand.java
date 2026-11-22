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

public class ProtectSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color AQUA = new Color(85, 255, 255);

    public ProtectSubcommand(ForgottenTempleMod plugin) {
        super("protect", "Toggle full protection mode (blocks, PvP, fall damage)");
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
        String state = parseStateArgument(ctx, "protect");

        boolean currentFullProtection = plugin.isProtectWorld()
                && plugin.isPvPDisabled()
                && plugin.isFallDamageDisabled()
                && !plugin.isItemLossEnabled()
                && !plugin.isNPCDamageable()
                && !plugin.isGodModeEnabled()
                && plugin.isProtectTeleporter()
                && plugin.isProtectElectrumTeleporter()
                && plugin.isProtectFragmentPortal();
        boolean enableFullProtection;

        if (state == null) {
            enableFullProtection = !currentFullProtection;
        } else if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("enable") || state.equalsIgnoreCase("true")) {
            enableFullProtection = true;
        } else if (state.equalsIgnoreCase("off")
                || state.equalsIgnoreCase("disable")
                || state.equalsIgnoreCase("false")) {
            enableFullProtection = false;
        } else {
            playerData.sendMessage(Message.raw("Usage: /ft protect [on/off]").color(YELLOW));
            return;
        }

        if (enableFullProtection) {
            plugin.setDisablePvP(true);
            plugin.setProtectWorld(true);
            plugin.setDisableFallDamage(true);
            plugin.setItemLoss(false);
            plugin.setNPCDamageable(false);
            plugin.setGodMode(false);
            plugin.setProtectTeleporter(true);
            plugin.setProtectElectrumTeleporter(true);
            plugin.setProtectFragmentPortal(true);
            playerData.sendMessage(Message.raw("Full protection enabled:").color(GREEN));
            playerData.sendMessage(
                    Message.raw("  PvP: OFF, Build: OFF, Fall Damage: OFF").color(AQUA));
            playerData.sendMessage(
                    Message.raw("  Item Loss: OFF, NPC Damage: OFF").color(AQUA));
            playerData.sendMessage(Message.raw("  Lab Teleporters, Electrum Teleporters, Fragment Portal: protected")
                    .color(AQUA));
        } else {
            plugin.setDisablePvP(false);
            plugin.setProtectWorld(false);
            plugin.setDisableFallDamage(false);
            plugin.setItemLoss(true);
            plugin.setNPCDamageable(true);
            plugin.setGodMode(false);
            plugin.setProtectTeleporter(false);
            plugin.setProtectElectrumTeleporter(false);
            plugin.setProtectFragmentPortal(false);
            playerData.sendMessage(Message.raw("Full protection disabled:").color(RED));
            playerData.sendMessage(
                    Message.raw("  PvP: ON, Build: ON, Fall Damage: ON").color(AQUA));
            playerData.sendMessage(
                    Message.raw("  Item Loss: ON, NPC Damage: ON").color(AQUA));
            playerData.sendMessage(Message.raw("  Lab Teleporters, Electrum Teleporters, Fragment Portal: breakable")
                    .color(AQUA));
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
