# Contributing

Bug reports, recommendations, implementations, original human-made pixel art and human-written **Cursed Findings** content are welcome. Afterdark Industries will do its best to review submissions. Open an issue to describe a problem or idea, or a focused pull request for an implementation. For security or privacy findings, follow the [Security policy](SECURITY.md).

## Start with the whole mod

Read the [Mod guide](docs/MOD_GUIDE.md) for all substance systems and their connections, then [Development](docs/DEVELOPMENT.md) for setup, source workflows and validation. Contributions can improve any existing area:

- Substance sessions, combinations, tolerance, dependence, recovery and physiological feedback, including older substances and their different consumption routes.
- Exploration loot, crops, world generation, crafting, processing utilities, item contents, vessels and capsule blends.
- Dealers, brokers, customers, bartenders, taverns, music, lighting, the journal, menus, localization, accessibility and desktop/VR presentation.
- Saves, compatibility, multiplayer, build tooling, tests and documentation.

For a report, explain what happened, what you expected, how to reproduce it and which mod/Minecraft/NeoForge versions you used. For rendering or input problems, include relevant client settings and other rendering mods; for VR, include Vivecraft version and headset/controller setup. Remove private information from logs or screenshots before attaching them.

## Human-made pixel art

Digital pixel art drawn by people would be greatly appreciated, with the goal of gradually replacing all AI-generated pixel art.

- Identify the runtime texture or item you are replacing. For a direct replacement, provide a PNG matching its dimensions and transparency so existing models and behavior remain consistent. Keep it readable at Minecraft's normal display scale.
- State that the artwork is human-made and identify references or adapted material. An editable source is welcome; no particular art application is required.
- Submit only work you can share under the [MIT license](LICENSE.txt). Record the runtime path, source, license and any required notices in [Asset provenance](docs/ASSET_PROVENANCE.md) and, where applicable, [Third-party notices](THIRD_PARTY_NOTICES.md).
- Give the public name or handle you want credited. Remove unintended personal details, local paths and identifying embedded metadata from both exports and editable sources.

## Human-written Cursed Findings

Original writing is welcome to replace the book's AI-generated content. This includes descriptions, effects, production chains and notes for **every substance**, plus the dependence/recovery and economy chapters. Clear, consistent writing and corrections to inaccurate in-game explanations are useful contributions too.

The current prose, production steps and recipe descriptions live in `src/main/java/com/trippycraft/guide/CursedFindingsRegistry.java`. Entry structures and identifiers live beside it in `guide/`; `CursedRecipeCodex.java` controls player discovery progress, and `client/CursedFindingsScreen.java` presents the pages. Use the [Mod guide](docs/MOD_GUIDE.md) to find the actual item, machine and substance behavior behind a page.

Keep writing accurate to the mod's fictional mechanics. Preserve entry IDs, discovery unlocks and recipe behavior for a prose replacement. Check the book in game for wrapping, scrolling and readability. Propose intentional gameplay or journal behavior changes separately and describe their effect clearly.

Submit your own human-written work, identify any adapted material and its license, and give your chosen public credit. Contributions use the project MIT license where you hold the necessary rights; preserve required outside notices and record provenance.

## Code changes and review

Explain the observable change, its purpose and the checks performed. Trace its relevant connections: acquisition, processing and item contents, server state, packets, client presentation, journal and saves. Follow the workflows and validation matrix in [Development](docs/DEVELOPMENT.md).

Preserve registry IDs, save aliases, physical-content/effect calibration and packet contracts unless your proposal explicitly changes them. The server owns gameplay state; client presentation consumes synchronized state. Network format changes require a protocol change and compatible client/server testing. Separate intentional balance or security changes from mechanical cleanup so their impact can be reviewed.

Vivecraft visual effects are supported, while movement, camera and other input-related substance effects are currently buggy. Report real headset/controller checks honestly. Automated GPU or GUI checks do not establish headset comfort or arbitrary modpack compatibility.

Dependency verification is strict. For an intentional dependency change, inspect the upstream artifact and review checksum changes; do not disable verification to make a build pass. Keep unrelated dependency and declared-version changes out of a focused contribution.

## Keep contributions clean

Do not commit `build/`, `.gradle/`, game instances, worlds, private configuration, logs, screenshots, temporary exports, bundled JDKs, recovery copies, credentials or personal paths. Put generated reports and previews under ignored `build/` output directories.

Public project attribution is **Afterdark Industries**; contributor credit uses each contributor's chosen public name or handle and does not replace their authorship. Only contribute material you can license, preserve upstream notices, and document outside material in [Asset provenance](docs/ASSET_PROVENANCE.md). Do not describe generated content as human-made. Public commit and account details should match the identity you intend to publish.
