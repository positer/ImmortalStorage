# ImmortalStorage unified core

This directory is the single maintenance workspace for loader- and
Minecraft-version-independent rules.  Version adapters consume these sources;
they must not fork or copy them.

Allowed here: deterministic probability, configuration models, transaction
planning, persistent identifiers, and other pure Java rules.

Forbidden here: `net.minecraft.*`, `net.neoforged.*`, registry/event/capability
calls, payload codecs, rendering, JEI/EMI entrypoints, or Numen/debug code.
