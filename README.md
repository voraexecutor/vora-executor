<div align="center">

# Vora

**A Java executor for Minecraft.**

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk&logoColor=white)
[![Discord](https://img.shields.io/badge/Discord-join-5865F2?logo=discord&logoColor=white)](https://discord.gg/pfZ33hjH6Q)

</div>

---

<!-- Add a screenshot or GIF here: <p align="center"><img src="docs/screenshot.png" width="800"></p> -->

Vora attaches an agent to a running Minecraft client, compiles your Java on the fly against the game's
own classes, and runs it on the client thread. Edit, press **EXECUTE**, repeat — no relaunching.

You write plain Java method bodies. `mc` (the live `MinecraftClient`) and `log(...)` are already in scope.

```java
log("Hello " + mc.player.getName().getString());
mc.player.sendMessage(net.minecraft.text.Text.literal("Hi from Vora!"), false);
```
## Requirements

- Java 17+
- Internet on first run (for mappings; cached afterwards)

## Writing scripts

You write only a method body; Vora wraps it. `mc` and `log(...)` are in scope, and `return;` stops early.

**`script.java` — runs once:**

```java
log("Pos: " + mc.player.getPos());
```

**`mixin.java` — runs every tick (~20×/s); keep it cheap:**

```java
if (mc.player == null) return;
if (mc.world.getTime() % 20 == 0) log("tick " + mc.world.getTime());
```

Rules: no class/main/package; `import`s allowed anywhere; runs on the client thread; **120s** timeout;
only classes already present in the game.

## The `mc` object

| Expression | Meaning |
| --- | --- |
| `mc.player` / `mc.world` | Player / world — `null` in menus |
| `mc.options` | Keybinds and settings |
| `mc.execute(runnable)` | Run on the client thread (required for GUI) |
| `mc.setScreen(screen)` | Open a screen |
| `mc.interactionManager` | Attack, interact, place blocks |
| `mc.world.getPlayers()` | Nearby players |

## Credits:

Opencode
Gemini

## Links

Built with [ASM](https://asm.ow2.io/) and [Eclipse ECJ](https://www.eclipse.org/ecj/).
Join the **[Discord](https://discord.gg/pfZ33hjH6Q)**.
