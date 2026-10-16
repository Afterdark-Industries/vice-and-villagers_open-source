# DMT fractal branch build

The ready-to-install [mod JAR](ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar) is in this repository's `artifacts/` folder. This is the build from the `codex/v2.2.0-dmt-rendering-review` branch for **Minecraft 1.21.1**, **NeoForge 21.1.249** and **Java 21**. The JAR is intentionally tracked here so it is available alongside this branch's source; other generated build output remains ignored.

Place the JAR in your instance's `mods` folder, replacing the previous Vice and Villagers JAR, then restart Minecraft. Keep one version of the mod installed.

The original DMT effects remain the default. To enable the optional 3D fractal realm, set these values in your instance's `config/trippycraft-client.toml`:

```toml
dmtFractalWorld = true
dmtFractalQuality = 1
```

Quality `0` is low, `1` is balanced, and `2` is high. Assign **Toggle DMT Fractal World** under **Options → Controls → Key Binds → Vice and Villagers** to switch modes in game; the binding starts unassigned and saves your preference. The generated realm is visual only, with Minecraft's existing blocks and collisions.

Build, isolated GPU shader checks and controller tests pass. Live Minecraft and headset testing remain outstanding. See the [mod guide](../docs/MOD_GUIDE.md#dmt) for behavior and [development guide](../docs/DEVELOPMENT.md#shader-and-audio-checks) for validation commands.

The adjacent [SHA256SUMS](SHA256SUMS) records the JAR's checksum. To rebuild and refresh this branch artifact from the repository root in PowerShell with Java 21 selected:

```powershell
.\scripts\build.ps1 build
Copy-Item -LiteralPath 'build/libs/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar' -Destination 'artifacts/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar' -Force
$artifactHash = (Get-FileHash -LiteralPath 'artifacts/ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar' -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath 'artifacts/SHA256SUMS' -Value "$artifactHash  ViceAndVillagers-2.2.0-backport-1.21.1-neoforge.jar" -Encoding ascii
```
