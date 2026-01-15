# Repo Map & Entry Points

## Scope
- Directory structure analysis of `decompiled/` and `meine_mods/`.
- Identification of build files and entry points.

## Evidence
- `decompiled/com/hypixel/hytale/` structure indicates the root of the game logic.
- `meine_mods/` contains three Gradle-based plugin projects: `hazyPlugin`, `deathmatchPlugin`, `LivingOrbis`.
- `build.gradle` files exist in each mod subdirectory.

## Findings

### Directory Structure
```
/
├── decompiled/                 # Decompiled Hytale Server/Client sources
│   └── com/hypixel/hytale/
│       ├── common/             # Shared logic
│       ├── logger/             # Logging infrastructure
│       └── server/             # Server-specific logic
├── meine_mods/                 # Custom Hytale Plugins
│   ├── hazyPlugin/             # Basic plugin (Test/Template)
│   ├── deathmatchPlugin/       # Complex Deathmatch minigame
│   └── LivingOrbis/            # Plugin integrating external services (Gemini)
└── docs/reverse-engineering/   # This knowledge base
```

### Entry Points
#### Hytale Server (Decompiled)
- **Root Package**: `com.hypixel.hytale`
- **Likely Entry Point**: Search required for `main` method in `com.hypixel.hytale.server`.

#### Plugins
- **Standard**: Plugins implement a main class extending a base plugin class (likely from Hytale API).
    - `hazyPlugin`: `de.hazy.HazyPlugin`
    - `deathmatchPlugin`: `de.hazy.deathmatch.DeathmatchPlugin`
    - `LivingOrbis`: `net.deinserver.livingorbis.LivingOrbisPlugin`

### Build System
- **Tool**: Gradle (Wrappers present in `meine_mods/*/gradlew`)
- **Config**: `build.gradle` files define dependencies and build tasks for plugins.

## Hypotheses
- The `decompiled` code is likely a partial or full dump of the server jar.
- Plugins rely on a `hytale-api` dependency, likely referenced in `build.gradle`.

## Open Questions
- What is the specific Main-Class for the server?
- Where are the dependencies (like the Hytale API jar) located or resolved from?

## Next Actions
1. Locate `public static void main` in `decompiled`.
2. Analyze `build.gradle` to find API references.
