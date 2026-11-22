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

public class StatusSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color AQUA = new Color(85, 255, 255);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color WHITE = new Color(255, 255, 255);

    public StatusSubcommand(ForgottenTempleMod plugin) {
        super("status", "Show current temple settings");
        this.plugin = plugin;
        requirePermission("forgottentemple.admin");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull PlayerRef playerData,
            @Nonnull World world) {
        playerData.sendMessage(Message.raw("=== Forgotten Temple Settings ===").color(GOLD));
        playerData.sendMessage(Message.raw(""));

        playerData.sendMessage(Message.raw("World Rules:").color(AQUA));
        sendSettingLine(playerData, "  [P] PvP", !plugin.isPvPDisabled());
        sendSettingLine(playerData, "  [B] Build", !plugin.isProtectWorld());
        sendSettingLine(playerData, "  [F] Fall Damage", !plugin.isFallDamageDisabled());
        sendSettingLine(playerData, "  [D] Item Drop/Loss", plugin.isItemLossEnabled());
        sendSettingLine(playerData, "  [N] NPC Damageable", plugin.isNPCDamageable());
        sendSettingLine(playerData, "  [G] God Mode", plugin.isGodModeEnabled());

        playerData.sendMessage(Message.raw(""));

        playerData.sendMessage(
                Message.raw("Block Protection (temple only, overrides Build):").color(AQUA));
        sendSettingLine(playerData, "  Entry Portal", plugin.isProtectEntryPortal());
        sendSettingLine(playerData, "  Exit Portal", plugin.isProtectExitPortal());
        sendSettingLine(playerData, "  Lab Teleporters", plugin.isProtectTeleporter());
        sendSettingLine(playerData, "  Electrum Teleporters", plugin.isProtectElectrumTeleporter());
        sendSettingLine(playerData, "  Fragment Portal", plugin.isProtectFragmentPortal());
        sendSettingLine(playerData, "  Memory/Statues", plugin.isProtectMemory());
        sendSettingLine(playerData, "  Spawn Area", plugin.isProtectSpawn());

        playerData.sendMessage(Message.raw(""));

        playerData.sendMessage(Message.raw("Global Overrides (all worlds):").color(AQUA));
        sendSettingLine(playerData, "  Break Entry Portal Anywhere", plugin.isAllowBreakEntryPortal());
        sendSettingLine(playerData, "  Break Exit Portal Anywhere", plugin.isAllowBreakExitPortal());

        playerData.sendMessage(Message.raw(""));

        playerData.sendMessage(Message.raw("Other Protections:").color(AQUA));
        sendSettingLine(playerData, "  Klop Merchant Invulnerable", plugin.isProtectKlopMerchant());
        sendSettingLine(playerData, "  PvP at Spawn Blocked", plugin.isPvPAtSpawnDisabled());

        playerData.sendMessage(Message.raw(""));

        playerData.sendMessage(Message.raw("WorldConfig Settings:").color(AQUA));
        sendSettingLine(playerData, "  World Closed", plugin.isWorldClosed());
        sendSettingLine(playerData, "  Mob Spawning Disabled", plugin.isMobsDisabled());
        sendSettingLine(playerData, "  NPCs Frozen", plugin.isNPCsFrozen());

        String weather = plugin.getForcedWeather();
        sendValueLine(
                playerData, "  Forced Weather", weather != null && !weather.isEmpty() ? weather : "None (dynamic)");

        String time = plugin.getFreezeTime();
        sendValueLine(
                playerData, "  Frozen Time", time != null && !time.isEmpty() ? formatTime(time) : "None (dynamic)");
        String worldLayout = plugin.getTempleWorldLayout();
        sendValueLine(playerData, "  Temple Layout", worldLayout != null ? worldLayout : "Not created yet");
        sendValueLine(playerData, "  TempleLayout (config, new worlds)", plugin.getTempleLayout());

        playerData.sendMessage(Message.raw(""));
        playerData.sendMessage(Message.raw("Protection Commands:").color(GRAY));
        playerData.sendMessage(
                Message.raw("  /ft protectentry|protectexit|protectteleporter|protectelectrum|protectfragment [on/off]")
                        .color(WHITE));
        playerData.sendMessage(
                Message.raw("  /ft protectmemory|protectspawn [on/off]").color(WHITE));
        playerData.sendMessage(Message.raw("  /ft breakentry|breakexit [on/off] (global override)")
                .color(WHITE));
        playerData.sendMessage(Message.raw(""));
        playerData.sendMessage(Message.raw("World Rule Commands:").color(GRAY));
        playerData.sendMessage(Message.raw("  /ft pvp|pvpspawn|build|falldamage|itemloss [on/off]")
                .color(WHITE));
        playerData.sendMessage(
                Message.raw("  /ft npcdamage|godmode|mobs|freezenpcs [on/off]").color(WHITE));
        playerData.sendMessage(Message.raw("  /ft weather [default/sunny/rain/storm/snow/none|AssetName]")
                .color(WHITE));
        playerData.sendMessage(Message.raw("  /ft time [day/night/HH:MM/none]").color(WHITE));
        playerData.sendMessage(
                Message.raw("  /ft reload | status | debug [on/off]").color(WHITE));
        playerData.sendMessage(Message.raw(""));
        playerData.sendMessage(Message.raw("Note: Block protection settings override Build OFF.")
                .color(GRAY));
        playerData.sendMessage(
                Message.raw("To break portals, use: /ft protectentry off").color(GRAY));
    }

    private void sendSettingLine(PlayerRef player, String name, boolean enabled) {
        Color statusColor = enabled ? GREEN : RED;
        String status = enabled ? "ON" : "OFF";
        player.sendMessage(Message.raw(name + ": " + status).color(statusColor));
    }

    private void sendValueLine(PlayerRef player, String name, String value) {
        player.sendMessage(Message.raw(name + ": " + value).color(GRAY));
    }

    private String formatTime(String time) {
        if ("day".equalsIgnoreCase(time)) {
            return "day (12:00)";
        } else if ("night".equalsIgnoreCase(time)) {
            return "night (03:00)";
        }
        return time;
    }
}
