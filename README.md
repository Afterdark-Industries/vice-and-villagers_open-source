# Vice and Villagers

Vice and Villagers **2.2.0**, by **Afterdark Industries**, is a Minecraft **1.21.1** mod for **NeoForge 21.1.249**. Explore, cultivate and craft fictional substances, trade with villagers, visit taverns, and discover their effects through a persistent simulation with desktop and optional Vivecraft presentation.

All substance quantities, effects and production chains are fictional gameplay values. They are not real-world dosing, preparation or medical instructions.

## DMT fractal branch download

This branch includes an optional generated 3D DMT fractal realm. The [built mod JAR](artifacts/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar) is in this repository's `artifacts/` folder, beside its [installation instructions, settings and checksum](artifacts/README.md). Open **Mods → Vice and Villagers → Config** to select the realm mode, intensity and quality. Existing DMT effects remain the default; the original shortcut bindings remain available.

## What the mod contains

The substance systems cover **cannabis, psilocybin, LSD, 2C-B, 4-HO-MET, AL-LAD, 2C-E, mescaline, DMT, salvia, cocaine, heroin, ketamine, MDMA, alcohol and nitrous oxide**. Koka and crack are routes within the cocaine system; different drinks share the alcohol system. Sessions can stack and interact, with tolerance, dependence, recovery, visual, audio and movement effects.

The rest of the mod connects those systems to exploration loot, wild plants and farming, five processing utilities, reusable vessels and capsule blends, dealers, market brokers, customers, village taverns, bartenders, music and lighting. **Cursed Findings** is the discovery journal for substances, production, recipes and the general economy and recovery systems.

**VR:** Vivecraft support works especially well with visual effects. Movement, camera and other input-related substance effects are currently buggy in VR. Changes in these areas need physical headset and controller testing; automated rendering checks alone do not establish correct interaction or comfort.

## Build and run

Install **Java 21** and set `JAVA_HOME` or put its `bin` directory on `PATH`. The checked-in Gradle wrapper supplies Gradle.

```powershell
.\scripts\build.ps1 build
.\scripts\build.ps1 runClient
```

The build creates `build/libs/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar`. The PowerShell helper handles project paths containing spaces or `&`. In a shell with a supported wrapper path, `./gradlew build` or `.\gradlew.bat build` also works.

Development clients use `run/` by default. To reuse a private instance, pass `-PdevRunDir=<absolute-folder>` to `runClient` or `runServer`. Keep generated output, game instances, worlds and private configuration out of version control. See [Development](docs/DEVELOPMENT.md) for setup, validation and contributor workflows.

The display name is Vice and Villagers. The runtime namespace and Java package remain `trippycraft` and `com.trippycraft` to preserve existing resources and saves. `/viceandvillagers status`, `sync` and operator-level `reset` are available; `/trippycraft` remains a compatibility alias.

## Documentation

| Guide | Purpose |
| --- | --- |
| [Mod guide](docs/MOD_GUIDE.md) | The whole mod: all substance systems, acquisition, utilities, village systems, presentation, saves and the source map |
| [Development](docs/DEVELOPMENT.md) | Build setup, how to work on each area, automated checks and manual desktop/VR validation |
| [Contributing](CONTRIBUTING.md) | Issues, recommendations, implementations, human-made artwork and Cursed Findings writing |
| [Asset provenance](docs/ASSET_PROVENANCE.md) | Asset origins, replacement contributions, licensing and public credit |
| [Security policy](SECURITY.md) | Reporting security and privacy problems |

## Contributions and license

Original **human-made digital pixel art** would be greatly appreciated: the goal is to replace all AI-generated pixel art over time. **Human-written Cursed Findings content** is also welcome to replace the book's AI-generated prose across every substance and its general chapters.

If you spot a code problem, have a recommendation, or want to contribute an implementation, please open an issue or submit a focused pull request. Afterdark Industries will do its best to review submissions. See [Contributing](CONTRIBUTING.md) for artwork, writing, validation and credit requirements.

See the [MIT license](LICENSE.txt), [third-party notices](THIRD_PARTY_NOTICES.md) and [asset provenance](docs/ASSET_PROVENANCE.md) for licensing and exceptions.
