# Security and privacy

Report security and privacy problems affecting Vice and Villagers, including unintended disclosure of account information, credentials or private paths; unauthorized file/network access; unsafe packet or persistence handling; and malicious dependencies or contributed code. Include the mod, Minecraft and NeoForge versions and whether the issue affects a client, dedicated server or development tool. The current declared release is 2.2.0 on Minecraft 1.21.1 / NeoForge 21.1.249.

## Report a problem

Use the hosting platform's private vulnerability-reporting channel when enabled. This source tree does not configure a dedicated confidential contact; a public issue is public. If confidential reporting is unavailable, request a private reporting route without posting secrets, private identifiers or an actionable malicious payload.

Provide a minimal reproduction, expected and observed behavior, prerequisites and relevant sanitized logs. Test only against instances and data you own or are authorized to test. Remove credentials, access tokens, real player identities, IP addresses and personal filesystem paths from public attachments. Do not publish someone else's private information as proof of a disclosure. No security response time is promised.

## Review the actual boundary

The server owns substance consumption, machine state, economy transactions and discovery. Client actions and data from packets, saves or imported worlds need validation at their receiving boundary. Optional client integrations and developer tools have different reachability from server gameplay; distinguish them when assessing a finding. The [mod guide](docs/MOD_GUIDE.md) maps these systems, and [Development](docs/DEVELOPMENT.md) describes checks and their limits.

Known gameplay issues include emerald-block currency inflation: the wallet values a block at ten emeralds while vanilla crafting uses nine. This remains existing behavior and must not be called fixed or exploit-free. Gameplay balance fixes need an explicit, separately reviewed proposal.

Repeated requests and persistence handling also warrant focused hardening review. A suspected risk or dependency advisory match is not evidence of arbitrary code execution or an owner-identity leak. Reports should establish the affected version, enabled feature, reachable input and practical effect before making that claim. Compatibility and reproduction tests belong with a proposed fix.

## Keep source and releases private where intended

Public project attribution is **Afterdark Industries**. Contributors choose their public credit. Keep account details, credentials, private prompts, recovery archives, player/world data, logs and machine-specific paths out of the repository and public attachments. Inspect exports and embedded image/audio metadata as well as visible text. Git author details and the hosting account are separate publication surfaces that also need the intended public identity.

Do not publish private identifier denylists or recovery evidence. Recheck source and packaged releases after changes; a source-only check does not inspect an older distributed JAR. Removing identifying metadata does not prevent unchanged images or music from being recognized in previous copies. See [Asset provenance](docs/ASSET_PROVENANCE.md).

Use the pinned Gradle wrapper, distribution checksum and strict dependency verification. Review new dependency provenance and checksum entries instead of bypassing verification. These checks protect declared build inputs; they do not establish that every dependency or contributed feature is safe. Preserve upstream legal notices and review intentional security/dependency changes with the affected gameplay, save and multiplayer contracts.
