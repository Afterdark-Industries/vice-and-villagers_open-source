# Third-party notices

Original Vice and Villagers contributions are licensed under [MIT](LICENSE.txt) by Afterdark Industries. That grant applies only to rights held by the project and does not relicense upstream software, mappings, game assets or trademarks.

## Gradle wrapper

`gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` originate from Gradle and are licensed under the [Apache License 2.0](licenses/Apache-2.0.txt). Their original copyright and license headers are preserved. See [Gradle source and license](https://github.com/gradle/gradle).

The wrapper downloads a separate Gradle distribution with its own dependency licenses and notices. Those files remain in the downloaded distribution; no Gradle distribution is vendored here.

## Build and runtime dependencies

Minecraft, NeoForge, Parchment mappings and Java are obtained by the build or installed by the user. They retain their upstream licenses. Refer to [Minecraft usage guidelines](https://www.minecraft.net/usage-guidelines), [NeoForge source](https://github.com/neoforged/NeoForge), [Parchment](https://parchmentmc.org/), and the chosen JDK distribution's legal directory.

Vivecraft integration uses its optional public API. Vivecraft itself is not bundled. See [Vivecraft source and license](https://github.com/Vivecraft/VivecraftMod).

Runtime resource references to `minecraft:` assets do not grant ownership of those assets. Synthetic GPU fixtures may read textures/shaders from the locally resolved Minecraft installation; those upstream assets are not part of the test fixtures distributed in this source tree.

Project artwork, models and tavern music are described in [Asset provenance](docs/ASSET_PROVENANCE.md). Required third-party attribution must remain when contributing or redistributing material.
