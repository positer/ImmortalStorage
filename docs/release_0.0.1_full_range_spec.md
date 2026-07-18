# Spec: Cultivation 0.0.1 full-range NeoForge release and cross-mod acceptance

## Objective

Release Cultivation `0.0.1` as separately built and tested NeoForge artifacts for every compatibility interval listed in `versions/supported_versions.json`, currently spanning Minecraft `1.21.1` through `26.2`. The implementation is composed from one loader-neutral/core source area plus exactly one NeoForge-version adapter. Every artifact must be tested in a real PCL2 client and dedicated/integrated server with Numen acting only as an external player through the official MCP and the separate test bridge.

The release is not complete until the required optional integrations are proven with the target mod absent, installed alone, in common pairs, and in the all-available-mod set. Every functional UI page must have a real-client screenshot for user review.

## Authoritative requirements

- `Goal.md` is the mutable product specification and takes precedence over this execution document.
- `versions/supported_versions.json` is the release-coordinate ledger. Only entries with `status=released` and `published=true` are support claims.
- The official NeoForge Maven metadata is the authority for pinned NeoForge coordinates. All 15 currently listed coordinates existed in the official repository on 2026-07-16; `26.2.0.18-beta` was then the newest 26.2 build, while the manifest still pinned `26.2.0.12-beta` and therefore requires a deliberate repin before its release build.
- Optional-mod behavior must use the target mod's public/official API for the exact target version. Reference-addon code may inform transaction boundaries but must not be copied into Cultivation.

## Tech stack

- JDK 21.
- NeoForge ModDevGradle integration projects, one per non-overlapping compatibility interval.
- Unified core: `project/cultivation-core`.
- Version adapters: `project/version-compat/neoforge/<coordinate>`.
- Optional integrations: isolated packages whose public/common entry points contain no target-mod types.
- Runtime QA: real PCL2 instances, official Numen MCP on `127.0.0.1:8765`, independent authenticated bridge on `127.0.0.1:8766`.

## Commands

- Validate release ledger:
  `python tools/version-matrix/version_matrix.py validate --manifest versions/supported_versions.json --workspace .`
- Run version-matrix tests:
  `python -m unittest discover -s tools/version-matrix/tests -v`
- Build one registered coordinate:
  `powershell -ExecutionPolicy Bypass -File tools/version-matrix/build-version.ps1 -Id <compatibility-id>`
- Active 1.21.1 clean gate:
  `cd project/neoforge-1.21.1-mdk && .\gradlew.bat clean check --no-daemon --max-workers=2 --console=plain`
- Texture audit:
  `python C:\Users\12252\.codex\skills\minecraft-texture-assets\scripts\audit_minecraft_textures.py project/neoforge-1.21.1-mdk/src/main/resources --modid cultivation`
- Runtime acceptance: launch the exact PCL2 instance, acquire a Numen body, perform the required player flow, inspect authoritative state, capture frame-buffer screenshots, and release the body in `finally`.

## Project structure

- `Goal.md` — living product requirements.
- `versions/` — machine-readable release intervals and schema.
- `project/cultivation-core/` — resource ledgers, amount bridges, atomic transfer plans and other version-neutral logic.
- `project/version-compat/neoforge/` — isolated API-break adapters and descriptors.
- `project/neoforge-*/` — exact-version integration projects that compose core + one adapter.
- `project/neoforge-1.21.1-mdk/src/main/java/.../compat/` — current optional integration implementations; later version projects must own their version-specific equivalents/adapters.
- `tools/version-matrix/` — ledger/artifact validators and build orchestration.
- `tools/numen-codex-bridge/` — external-only input, screenshot and test-fixture bridge.
- `archive/<timestamp>-release-0.0.1-*/` — per-coordinate build, runtime, logs, screenshots and hashes.

## Code style and architecture

```java
// Always-loaded code mentions only a class name and loads it after an exact
// mod-id/version gate. Optional API types stay inside the isolated package.
if (OptionalModGate.matches("botania", supportedRange)) {
    OptionalBootstrap.invoke("...compat.botania.BotaniaCompat", modBus);
}
```

- Server state and resource ledgers are authoritative.
- Every transfer follows SIMULATE -> reserve/execute -> observe/settle -> refund, with no deletion or duplication on partial failure.
- Long-valued storage saturates safely when exposed through int-valued APIs.
- One resource channel key must represent exactly one resource system. Ars Source, Iron's Spells mana, Botania mana, FE, Mekanism chemicals and Industrial Foregoing Souls may not alias one another.
- The common mod and unrelated integration modules must load when any optional mod is absent.

## Required integrations

1. AE2: item/fluid storage-bus visibility, exchange-cell mount/deduplication, simulate/execute parity and source bulk insertion.
2. Refined Storage: exchange disk mount/deduplication and item/fluid access through the official RS API.
3. NeoForge FE and compatible power mods: owner-bound Xianqiao storage, Xianqiao Interface per-face input/output, personal-realm device refill and configurable Immortal-Yuan conversion.
4. Mekanism: official strict-energy and chemical handlers; staff direction/input-limit interaction modeled on established wireless configurator behavior without copying Draconic Evolution code.
5. Flux Networks: FE-compatible transfer plus any required official network API boundary.
6. Ars Nouveau Source: independent Source ledger, interface capability and personal-realm storage refill.
7. Iron's Spells 'n Spellbooks mana: separate official API integration; never routed through Ars Source.
8. Botania: mana receiver/pool and spark interaction, independent long ledger with safe int projection, personal-realm pool refill.
9. Industrial Foregoing Souls: `industrialforegoingsouls:soul`, `soul_laser_base`, `soul_network_pipe` and `soul_surge` acquisition/transfer/storage path.

## Xianqiao Interface mixed external-resource cache

- The nine configuration/cache pairs are one mixed resource model, not separate pages. A slot may target an item, fluid, Mekanism chemical, FE energy, Ars Nouveau Source, Botania mana, Industrial Foregoing Soul or another explicitly registered optional channel.
- Every slot stores an exact resource identity, authoritative long target amount, authoritative long cached amount and a six-bit output-face mask. Duplicate resource identities in different slots remain independent and are not aggregated by configuration.
- The existing global face mode remains authoritative and visually stays in two horizontal rows (`UP/NORTH/DOWN`, then `WEST/SOUTH/EAST`). A cached slot is exposed or actively pushed on a face only when both conditions hold: the face mode is `PUSH`, and that slot's output-face mask includes the face.
- The amount modal is also the per-slot face-mask editor. It uses the same two-row direction order and clearly distinguishes enabled and disabled faces. Saving amount and mask is one revision-checked server transaction.
- `PULL` accepts the target mod's official resource capability into the owner ledger; `PUSH` exposes only configured real cache whose slot mask enables that face; `DISABLED` and unsided queries fail closed.
- Optional resource identities, icons and capability adapters are registered by isolated mod modules. Absent mods hide or disable their targets without leaving undecodable hard references in common NBT or packets.

## AE2 Matter Condenser throughput acceptance

- Ordinary `PUSH` deliberately follows the target's standard ItemHandler boundary. High-volume Matter Condenser input requires explicit `BYPASS_PUSH`, which must resolve `AECapabilities.ME_STORAGE` to `CondenserMEStorage` and submit the full face-local long request.
- Runtime acceptance must record source `fluxLimit`, actual side mode, side fault/in-flight state, target ItemHandler class, target MEStorage class, condenser mode/required power, installed storage capacity, stored power and output-slot state before and after the measured interval.
- A blocked/full condenser output is a target backpressure result, not a source rate failure. The acceptance fixture must continuously drain output or measure only until the first full output stack, and must compare ordinary `PUSH` with `BYPASS_PUSH` using the same configured condenser.

## Testing strategy

- RED: each missing registration, transaction, version boundary or reproduced runtime failure first receives a test that fails on the current source.
- GREEN: implement the smallest server-authoritative behavior that satisfies the requirement.
- REFACTOR: isolate target-mod types and share only loader-neutral transaction logic.
- Unit tests cover saturation, stage gates, direction policies, deduplication and rollback.
- Integration tests compile/run with each target API present and verify absent-mod class loading.
- Real-client/server tests prove actual block/network behavior; source scans and mocks alone cannot satisfy runtime acceptance.
- Each coordinate must run: base/no optional mods; each available integration alone; common pairs; all available integrations; dedicated server; integrated client; Numen single- and multiplayer player flows.

## Release gates

For every manifest entry:

1. Exact adapter directory, descriptor and integration project exist.
2. Core + exactly one adapter composes successfully.
3. Unit/integration/full build and production-boundary checks pass.
4. Artifact filename, embedded adapter marker, Minecraft range and NeoForge range match the ledger.
5. Real PCL2 client and server both start and save cleanly.
6. Required core gameplay pages are exercised and captured.
7. Cross-mod matrix is recorded with authoritative logs and state inspection.
8. Artifact SHA256 and rollback predecessor are archived.
9. Only after all gates pass may the entry become `released` and `published=true`.

## Screenshot deliverables

- Kongqiao/Xianqiao terminal: storage, Craft, Furnace and Realm states.
- Xianqiao Interface: mixed targets/cache, amount modal and all six face modes.
- Source Vein page and rate editor.
- Immortal Furnace, World Shard Miner and Treasure Basin pages.
- AE2/RS disk placement, accepted/mounted state and terminal contents.
- FE/Mekanism, Ars, Iron's Spells, Botania and Souls resource pages or authoritative device GUIs showing before/after transfer.
- `zh_cn/en_us` and supported GUI scales required by `Goal.md` for the current UI baseline; version-specific visual changes receive separate goldens.

## Boundaries

- Always: preserve existing worlds and full Data Components; use exact owner UUIDs; keep optional dependencies isolated; archive proof before support claims.
- Ask first: destructive storage migration, mandatory dependency, publishing to a third-party account that requires new credentials, or changing the product version.
- Never: add Numen/MCP/debug interfaces to Cultivation; mark a planned/untested coordinate released; fabricate third-party availability; use Ars Source for Iron's mana; silently delete/duplicate resources; copy restricted addon art/code.

## Current audited state (2026-07-16)

- Release ledger entries: 15.
- Buildable adapters/integration projects: 1 (`1.21.1 / 21.1.235`).
- PCL2 instances: 1 (`1.21.1-NeoForge_21.1.235`).
- Published/released entries: 0.
- AE2 and RS have substantive code/tests, but the requested final real-network acceptance matrix is incomplete.
- Botania has adapter code but is not registered by `CompatManager` and its live storage resolver is not installed.
- Mekanism is only an API-presence descriptor; no energy/chemical capability transaction implementation exists.
- Flux Networks, Ars Nouveau, Iron's Spells and Industrial Foregoing Souls do not yet have correct target modules.

## Open risks

- Some third-party mods may not publish builds for every Minecraft/NeoForge interval. Such absence must be recorded from the official project source and cannot be converted into a false compatibility claim.
- Minecraft/NeoForge API changes between 1.21.1 and 26.2 are large enough that source copying plus version-string replacement is not an acceptable adapter strategy.
- `26.2` is currently a beta line and must remain a preview artifact unless upstream becomes stable before its release gate.
