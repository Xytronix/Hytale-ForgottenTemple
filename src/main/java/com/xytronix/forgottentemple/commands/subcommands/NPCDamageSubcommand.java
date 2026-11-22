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

public class NPCDamageSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);

    public NPCDamageSubcommand(ForgottenTempleMod plugin) {
        super("npcdamage", "Toggle whether NPCs can be damaged");
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
        String state = parseStateArgument(ctx, "npcdamage");

        boolean currentEnabled = plugin.isNPCDamageable();
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
            playerData.sendMessage(Message.raw("Usage: /ft npcdamage [on/off]").color(YELLOW));
            return;
        }

        if (newEnabled == currentEnabled) {
            if (currentEnabled) {
                playerData.sendMessage(Message.raw("NPC damage is already enabled (NPCs can be hurt).")
                        .color(YELLOW));
            } else {
                playerData.sendMessage(Message.raw("NPC damage is already disabled (NPCs invulnerable).")
                        .color(YELLOW));
            }
            return;
        }

        plugin.setNPCDamageable(newEnabled);

        if (newEnabled) {
            playerData.sendMessage(
                    Message.raw("NPC damage enabled - NPCs can now be hurt.").color(RED));
        } else {
            playerData.sendMessage(Message.raw("NPC damage disabled - NPCs are now invulnerable.")
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
