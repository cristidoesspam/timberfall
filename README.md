# Timberfall

Chop one log and the whole tree comes down. Fabric mod for **Minecraft 26.3**.

## Install

1. Fabric Loader 0.19.5 or newer for 26.3 (Java 25)
2. [Fabric API](https://modrinth.com/mod/fabric-api) `0.163.0+26.3` or newer
3. Drop `timberfall-1.0.1.jar` into your `mods` folder
4. Optional: [Mod Menu](https://modrinth.com/mod/modmenu) 21.0.0 for a Config button

Works in singleplayer and on servers. Install it on the server for the felling itself; players who also
have it on their client use their own settings. Players without it get the server's `config/timberfall.json`.

## Settings

Open them from Mod Menu, or bind **Open Timberfall settings** in Controls. **Toggle Timberfall** is a quick
on/off key. Both are unbound by default. Settings are saved in `config/timberfall.json`.

| Setting | What it does |
| --- | --- |
| Tree Felling | Master switch |
| Mode | **Instant**: the tree disappears the moment you break a log. **Block by Block**: breaks one block at a time with a delay, starting at the log you hit |
| Delay | Block by Block only. 1 to 20 ticks between blocks (20 ticks = 1 second). Leaves go in quick batches |
| Auto Pickup | Drops go straight into your inventory (the rest drops if it is full) |
| Use Axe Durability | On: 1 durability per log. Off: free |
| Protect Axe | With durability on, if the axe cannot survive the whole tree only the one log breaks, and an action bar message tells you why. Big trees need a lot of durability (a big jungle tree is 100+ logs), so a wooden, stone, gold or copper axe will often hit this. Turn it off to fell anyway |
| Replant | **Off**, **My Saplings** (uses a sapling from the tree's drops or your inventory) or **Free** (always plants one) |
| Break Leaves | Also remove the tree's natural leaves |
| Require Axe | Only fell while holding an axe |
| Require Leaves | Only fell log clusters with natural leaves attached, so log cabins are safe |
| Sneaking | **Chop One**: sneak to break a single log. **Fell**: only fell while sneaking. **Ignore** |
| Max Tree Size | Log clusters bigger than this break normally (with an action bar message). 16 to 2048, default 512. Trees whose logs touch count as one cluster, so dense jungles and dark forests may need a higher limit |

## Build

Needs JDK 25.

```
./gradlew build
```

The jar is in `build/libs/`.
