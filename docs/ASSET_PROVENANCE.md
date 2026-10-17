# Asset provenance

Public project attribution is **Afterdark Industries**. Contributors retain authorship and are credited by their chosen public name or handle. The [MIT license](../LICENSE.txt) applies to rights held by the project; it does not relicense upstream material or establish copyright in generated output. Preserve the exceptions in [Third-party notices](../THIRD_PARTY_NOTICES.md).

## Current material

| Material | Origin and contributor direction |
|---|---|
| Pixel art | The current placeholder pixel artwork was generated with ChatGPT. Original human-made digital pixel art is welcome to replace it progressively. |
| Tavern music | The nineteen runtime tracks were generated with Suno while an active Pro/Premier subscription was held, as confirmed by the project maintainer. They are placeholders; do not include private account details or source identifiers in public attribution. |
| Cursed Findings prose | The book includes AI-generated content. Human-written descriptions, effects, production explanations and notes are welcome for every substance and the general system chapters. |
| Code, models, structures and gameplay data | Maintained project sources include Java, JSON, templates, models and authoring tools. Generated runtime models/structures and their required companions remain game inputs; a generator is not a replacement for every output the loader needs. |
| Regression fixtures | Small project source/shader baselines under `tests/fixtures/` support retained validators. They are maintenance inputs. |
| Upstream material | Minecraft references, Gradle wrapper files and separately resolved dependencies keep their upstream terms. Refer to third-party notices; a reference to a vanilla resource does not establish project ownership. |

Generated content should be described honestly as generated. Historical production provenance is not a guarantee of exclusivity or rights in outside reference material. Submit only material you have the necessary rights to share, and retain required attribution for adaptations.

## DMT Powder and Ancient Pipe icons

The maintainer supplied the two runtime icons below for this feature. Their original artist, generation method and any outside source license were not separately specified; this record does not assert additional authorship or licensing rights.

| Runtime asset | Supplied artwork and processing |
|---|---|
| `assets/trippycraft/textures/item/dmt_powder.png` | The first supplied image: iridescent DMT powder. Original 1254×1254 pixel data and transparency retained; embedded content-credential metadata omitted. |
| `assets/trippycraft/textures/item/ancient_pipe.png` | The second supplied image: empty Ancient Pipe. Original 1254×1254 pixel data and transparency retained; embedded content-credential metadata omitted. All loaded pipe models reuse this texture and identify their contents through item names/tooltips. |

The third supplied image was explicitly excluded and is not included in runtime assets. The DMT-loaded Bong reuses the existing `bong_loaded.png` texture. No replacement or additional loaded-pipe artwork was generated.

## Record a contribution

For each new or replacement asset or book contribution, record:

- Its runtime path or book entry, and what it replaces.
- The creator's chosen public name or handle.
- Whether it is human-made or generated, with any outside references/adaptations identified.
- The license, redistribution permission and required notices for included outside material.
- The relevant editable source when available, after removing unintended private information.

Use [Contributing](../CONTRIBUTING.md) for pixel dimensions/transparency, writing accuracy and submission guidance. Check [Development](DEVELOPMENT.md) for model loading, book layout, audio and music-reactive validation. A track replacement can affect playback timing and reactive visuals; a prose replacement must preserve recipe/discovery information unless an intentional behavior change is proposed.

## Privacy

Keep private account details, source UUIDs, prompts, personal paths and recovery records out of exports and editable sources. Inspect PNG/OGG metadata and public commit details as well as visible content. Use public creator credits instead of personal identity.

Images and music can still be matched to earlier copies by their content after metadata is removed. Metadata sanitation therefore does not guarantee anonymity. Runtime player UUIDs, compatibility namespaces and the in-game codex are gameplay identifiers, rather than project-owner attribution.
