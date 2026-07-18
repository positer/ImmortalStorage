# Version support and composition policy

Cultivation uses product version `0.0.1`.  Product features are maintained once
in the unified core.  A released JAR is composed from that core and exactly one
NeoForge compatibility adapter.

## Hard compatibility boundary

- Every Minecraft or NeoForge API breakpoint creates a new adapter and a new,
  non-overlapping exact/ranged artifact.  A JAR may never cross an unverified
  breakpoint.
- Compatibility adapters contain registration, event, capability, payload,
  loot-context, rendering and recipe-viewer glue only.  They may not fork core
  business rules.
- Every artifact has its own clean build, unit/contract tests, dedicated-server
  startup, external Numen singleplayer and multiplayer flow, JEI/EMI matrix,
  production-boundary audit, hash and archive evidence.
- Beta NeoForge lines can only produce preview artifacts.
- `versions/supported_versions.json` is authoritative.  Only entries marked
  `released` and `published=true` may be described as supported.

When an API break is found inside an existing line, close the old interval at
the last verified build, create a sibling adapter, and rerun the entire matrix.
Never widen a range merely because compilation succeeds.
