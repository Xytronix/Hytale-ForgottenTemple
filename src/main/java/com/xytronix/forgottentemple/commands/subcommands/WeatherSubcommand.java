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
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public class WeatherSubcommand extends AbstractPlayerCommand {
    private final ForgottenTempleMod plugin;

    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color AQUA = new Color(85, 255, 255);
    private static final Color GRAY = new Color(170, 170, 170);

    private static final Map<String, String> WEATHER_PRESETS = new LinkedHashMap<>();

    static {
        WEATHER_PRESETS.put("default", "ForgottenTemple_Custom");
        WEATHER_PRESETS.put("custom", "ForgottenTemple_Custom");

        WEATHER_PRESETS.put("temple", "Forgotten_Temple");
        WEATHER_PRESETS.put("base", "Forgotten_Temple");

        WEATHER_PRESETS.put("sunny", "Zone1_Sunny");
        WEATHER_PRESETS.put("clear", "Zone1_Sunny");
        WEATHER_PRESETS.put("rain", "Zone1_Rain");
        WEATHER_PRESETS.put("rainy", "Zone1_Rain");
        WEATHER_PRESETS.put("storm", "Zone1_Storm");
        WEATHER_PRESETS.put("stormy", "Zone1_Storm");
        WEATHER_PRESETS.put("thunder", "Zone1_Storm");
        WEATHER_PRESETS.put("foggy", "Zone1_Foggy_Light");
        WEATHER_PRESETS.put("fog", "Zone1_Foggy_Light");
        WEATHER_PRESETS.put("cloudy", "Zone1_Cloudy_Medium");

        WEATHER_PRESETS.put("snow", "Zone3_Snow");
        WEATHER_PRESETS.put("snowy", "Zone3_Snow");
        WEATHER_PRESETS.put("blizzard", "Zone3_Snow_Storm");
        WEATHER_PRESETS.put("snowstorm", "Zone3_Snow_Storm");

        WEATHER_PRESETS.put("sandstorm", "Zone2_Sand_Storm");
        WEATHER_PRESETS.put("desert", "Zone2_Sunny");
        WEATHER_PRESETS.put("haze", "Zone2_Desert_Haze");

        WEATHER_PRESETS.put("ash", "Zone4_AshWastes");
        WEATHER_PRESETS.put("volcanic", "Zone4_Lava_Fields");
        WEATHER_PRESETS.put("spooky", "Zone4_Spooky");

        WEATHER_PRESETS.put("void", "Void");
        WEATHER_PRESETS.put("bloodmoon", "Blood_Moon");
        WEATHER_PRESETS.put("blood", "Blood_Moon");
        WEATHER_PRESETS.put("creative", "Creative_Hub");

        WEATHER_PRESETS.put("skylands", "Skylands/Skylands_Sunny");
        WEATHER_PRESETS.put("mystic", "Skylands/Skylands_Mystic_Forest_Cloudy");

        WEATHER_PRESETS.put("none", "");
        WEATHER_PRESETS.put("off", "");
        WEATHER_PRESETS.put("dynamic", "");
        WEATHER_PRESETS.put("disable", "");
    }

    public WeatherSubcommand(ForgottenTempleMod plugin) {
        super("weather", "Set forced weather (use preset name or asset ID)");
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
        String weatherArg = parseStateArgument(ctx, "weather");

        if (weatherArg == null || weatherArg.isEmpty()) {
            showCurrentWeather(playerData);
            return;
        }

        String resolvedWeather = resolveWeather(weatherArg);

        if (resolvedWeather == null) {
            resolvedWeather = weatherArg;
            playerData.sendMessage(
                    Message.raw("Using weather asset: " + resolvedWeather).color(YELLOW));
        }

        if (resolvedWeather.isEmpty()) {
            plugin.setForcedWeather(null);
            playerData.sendMessage(Message.raw("Forced weather disabled - weather is now dynamic.")
                    .color(GREEN));
        } else {
            plugin.setForcedWeather(resolvedWeather);
            playerData.sendMessage(
                    Message.raw("Forced weather set to: " + resolvedWeather).color(GREEN));
        }
    }

    private void showCurrentWeather(PlayerRef playerData) {
        String current = plugin.getForcedWeather();

        if (current == null || current.isEmpty()) {
            playerData.sendMessage(Message.raw("Weather: ").color(AQUA));
            playerData.sendMessage(Message.raw("  Dynamic (not forced)").color(YELLOW));
        } else {
            String presetName = findPresetName(current);
            playerData.sendMessage(Message.raw("Weather: ").color(AQUA));
            if (presetName != null) {
                playerData.sendMessage(
                        Message.raw("  " + current + " (" + presetName + ")").color(GREEN));
            } else {
                playerData.sendMessage(Message.raw("  " + current).color(GREEN));
            }
        }

        playerData.sendMessage(Message.raw("").color(GRAY));
        playerData.sendMessage(
                Message.raw("Usage: /ft weather <preset|asset_name|none>").color(YELLOW));
        playerData.sendMessage(Message.raw("Presets: default, temple, sunny, rain, storm, snow, fog, void, none")
                .color(GRAY));
        playerData.sendMessage(Message.raw("Or use any weather asset name (e.g., Zone1_Rain, Zone3_Snow_Heavy)")
                .color(GRAY));
    }

    private String findPresetName(String weatherAsset) {
        for (Map.Entry<String, String> entry : WEATHER_PRESETS.entrySet()) {
            if (entry.getValue().equals(weatherAsset)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String resolveWeather(String input) {
        if (input == null) {
            return null;
        }
        String lower = input.toLowerCase();
        return WEATHER_PRESETS.get(lower);
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
