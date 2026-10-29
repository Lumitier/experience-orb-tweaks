# Experience Orb Tweaks

Client-side [Fabric](https://fabricmc.net/) mod for Minecraft **1.21.11**.
Makes experience orbs fully configurable — size, colours, pulse, opacity,
shadow — and finally shows **how much XP an orb is actually worth**.

## Why

Vanilla stops growing orbs at 2477 XP: beyond that, `getOrbSize()` always
returns texture tier 10. With [Clumps](https://modrinth.com/mod/clumps) or
any other merging mod, a single orb quickly carries many times that — but
looks exactly like every other large orb.

This mod reads the real value and makes it visible, through size, through
colour, or both.

## Features

- **Size, opacity and self-illumination** fully adjustable
- **Two colour pickers with hex input** — a base colour and a pulse colour,
  with a fade in between
- **Rainbow mode** with its own speed and opacity
- **Size and colour by XP value** on a logarithmic scale — spot a big clump
  at a glance
- **Scale the shadow** with the orb, or keep it at vanilla size
- **Hide orbs entirely**
- Purely client-side: works on any server, no gameplay changes, no network
  packets

## Configuration

In game via **Mods → Experience Orb Tweaks → ⚙** (requires Mod Menu).
Every setting applies instantly, no restart needed.

| Tab | Group | Options |
|---|---|---|
| General | — | Mod on/off |
| Experience Orbs | General Settings | Render orbs, self-illumination |
| | Scale Settings | Scale, scale shadow |
| | Pulse Settings | Should pulse, pulse speed |
| | Color Settings | Color, pulse color |
| | Rainbow Settings | Rainbow, opacity, speed |
| | XP Value Settings | Scale by value, color by value, max value |

Available in **English** and **German** — the screen follows your game
language.

Settings live in `config/xporbtweaks.json`. Invalid values are clamped to
valid ranges on load, and a broken file falls back to the defaults.

## Working with Clumps

The two mods solve different halves of the same problem and never touch each
other:

| | Clumps | Experience Orb Tweaks |
|---|---|---|
| Side | Server | Client |
| Target class | `ExperienceOrb` (entity) | `ExperienceOrbEntityRenderer` |
| What it does | Fewer entities, less lag | How the orb looks |

Clumps is **not** required. Without it, XP values stay small and the
value-based options barely show.

For testing, Clumps is declared as `modRuntimeOnly` in `build.gradle`. It is
not bundled into the released jar and is not a dependency of this mod.

## Requirements

- Minecraft **1.21.11**
- **Fabric Loader 0.20.1** or newer
- [YetAnotherConfigLib](https://modrinth.com/mod/yacl)
- [Mod Menu](https://modrinth.com/mod/modmenu) *(optional — needed for the
  settings button)*

## Credits

The idea goes back to **Custom Experience Orbs** by
[Walksy](https://modrinth.com/mod/custom-experience-orbs) (MIT, Fabric,
1.21–1.21.5). This mod is a complete reimplementation for 1.21.11 — no code
was taken from it — and extends the concept with value-based rendering and
compatibility with orb-merging mods.

[Clumps](https://modrinth.com/mod/clumps) by BlameJared (MIT) is used only
as a test dependency.

## License

[Apache License 2.0](LICENSE). The source is open so server admins can
verify the mod does nothing beyond rendering.
