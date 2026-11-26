# Forgotten Temple

Hytale server mod that turns the Forgotten Temple instance into a persistent, shared world.

## Install

Requires Hytale Server 0.8.0-pre.7 or newer. For Hytale 0.7 use the `main` branch.

Put `Hytale-ForgottenTemple-1.1.1-beta.1.jar` in the server's `mods` folder. To build it yourself, run `mvn clean verify` (JDK 25, Maven 3.9+).

## Temple layout

`TempleLayout` in `config.json` picks the temple a new `ForgottenTemple` world is built from.

| `TempleLayout` | Temple |
|---|---|
| `Chapter1` (default) | Chapter I temple, the one the vanilla portal opens |
| `Chapter0` | The temple from before Chapter I |

Existing worlds are never reset. To switch, stop the server, back up the universe, move `universe/worlds/ForgottenTemple` out of `worlds` and restart. The new world gets a new UUID.

## Commands

All commands need `forgottentemple.admin`. From the console, add `--player <name>`. Toggles take `on` or `off`.

| Command | Description |
|---|---|
| `/ft pvp`, `/ft pvpspawn` | PvP in the temple, PvP at spawn |
| `/ft build` | Block breaking and placing |
| `/ft falldamage`, `/ft itemloss`, `/ft npcdamage`, `/ft godmode` | Fall damage, item loss on death, NPC damage, player invulnerability |
| `/ft closed` | Block entry to the temple |
| `/ft mobs`, `/ft freezenpcs` | Mob spawning, NPC movement |
| `/ft weather [preset/asset/none]` | Forced weather |
| `/ft time [day/night/HH:MM/none]` | Frozen time |
| `/ft breakentry`, `/ft breakexit` | Allow breaking temple portals in all worlds |
| `/ft protectentry`, `/ft protectexit` | Protect the entry and exit portals |
| `/ft protectteleporter`, `/ft protectelectrum`, `/ft protectfragment` | Protect Goblin Lab teleporters, Electrum Teleporters, the fragment portal |
| `/ft protectspawn`, `/ft protectmemory`, `/ft protectklop` | Protect the spawn area, memory statues, Klop merchants |
| `/ft protect` | All protections at once |
| `/ft debug`, `/ft reload`, `/ft status` | Debug logging, reload `config.json`, show settings |

## Permissions

| Permission | Grants |
|---|---|
| `forgottentemple.admin` | The `/ft` command |
| `forgottentemple.bypass` | Ignore the mod's block protection and enter a closed temple. Building still needs `/ft build on`. |
