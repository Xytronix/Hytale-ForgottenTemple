package com.xytronix.forgottentemple.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.xytronix.forgottentemple.ForgottenTempleMod;
import com.xytronix.forgottentemple.commands.subcommands.*;

public class ForgottenTempleCommand extends AbstractCommandCollection {

    public ForgottenTempleCommand(ForgottenTempleMod plugin) {
        super("ft", "Forgotten Temple management commands");
        requirePermission("forgottentemple.admin");

        addSubCommand(new PvpSubcommand(plugin));
        addSubCommand(new PvPSpawnSubcommand(plugin));
        addSubCommand(new BuildSubcommand(plugin));
        addSubCommand(new FallDamageSubcommand(plugin));
        addSubCommand(new ItemLossSubcommand(plugin));
        addSubCommand(new NPCDamageSubcommand(plugin));
        addSubCommand(new GodModeSubcommand(plugin));

        addSubCommand(new ClosedSubcommand(plugin));
        addSubCommand(new MobsSubcommand(plugin));
        addSubCommand(new FreezeNPCsSubcommand(plugin));
        addSubCommand(new WeatherSubcommand(plugin));
        addSubCommand(new TimeSubcommand(plugin));

        addSubCommand(new ProtectSpawnSubcommand(plugin));
        addSubCommand(new ProtectMemorySubcommand(plugin));
        addSubCommand(new ProtectKlopSubcommand(plugin));
        addSubCommand(new ProtectTeleporterSubcommand(plugin));
        addSubCommand(new ProtectElectrumTeleporterSubcommand(plugin));
        addSubCommand(new ProtectFragmentPortalSubcommand(plugin));
        addSubCommand(new ProtectEntryPortalSubcommand(plugin));
        addSubCommand(new ProtectExitPortalSubcommand(plugin));
        addSubCommand(new BreakEntrySubcommand(plugin));
        addSubCommand(new BreakExitSubcommand(plugin));

        addSubCommand(new ProtectSubcommand(plugin));
        addSubCommand(new DebugSubcommand(plugin));
        addSubCommand(new ReloadSubcommand(plugin));
        addSubCommand(new StatusSubcommand(plugin));
    }
}
