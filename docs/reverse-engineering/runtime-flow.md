# Runtime Flow & Plugin Lifecycle

## Scope
- Analysis of Plugin Initialization, Execution, and Shutdown.
- Understanding the `PluginBase` and `JavaPlugin` hierarchy.
- Search for Server Entry Point (`HytaleServer`).

## Evidence
- `de.hazy.HazyPlugin` extends `JavaPlugin`.
- `JavaPlugin` extends `PluginBase` (Found in `decompiled/.../server/core/plugin/`).
- `PluginBase` imports `com.hypixel.hytale.server.core.HytaleServer`.
- **CRITICAL**: `HytaleServer.java` is **NOT** present in the `decompiled` directory.
- `PluginBase` manages `PluginState` (NONE, SETUP, START, ENABLED, SHUTDOWN, DISABLED).

## Findings

### Missing Server Core
The `decompiled` directory appears to be a **partial dump** or an **SDK/API artifact** rather than the full server source.
- `HytaleServer` is referenced but missing.
- No `main` method found in the scanned files.
- The `decompiled` package structure `com.hypixel.hytale.server.core` contains `plugin`, `command`, and `receiver`, but misses the main application logic.

### Plugin Lifecycle
The Hytale Server manages plugins through a defined state machine (`PluginState`).

1.  **Instantiation**: Plugin is loaded by `PluginClassLoader` (referenced in `JavaPlugin`).
2.  **Configuration Loading**: `preLoad()` loads configs defined via `withConfig()`.
3.  **Setup (`setup()`)**:
    - State transitions to `SETUP`.
    - Plugins register commands, events, and other static resources here.
    - `HazyPlugin` registers its command in `setup()`.
4.  **Start (`start()`)**:
    - State transitions to `START` then `ENABLED`.
    - Used for logic that requires the server to be fully initialized.
    - `JavaPlugin` handles Asset Pack registration here.
5.  **Shutdown (`shutdown()`)**:
    - State transitions to `SHUTDOWN`.
    - Resources are cleaned up.

### Core Components exposed to Plugins
`PluginBase` provides access to several registries, indicating the server's architecture:

| Component | Accessor | Purpose |
| :--- | :--- | :--- |
| **CommandRegistry** | `getCommandRegistry()` | Register chat commands. |
| **EventRegistry** | `getEventRegistry()` | Subscribe to server events (`EventBus`). |
| **TaskRegistry** | `getTaskRegistry()` | Scheduling tasks (ticks/async). |
| **AssetRegistry** | `getAssetRegistry()` | Managing assets. |
| **EntityRegistry** | `getEntityRegistry()` | Managing entities. |

## Hypotheses
- The `decompiled` folder is likely the `hytale-common` and parts of `hytale-server` that are exposed to plugin developers (the API).
- The actual server runtime (`HytaleServer` class, main loop, network handling) is in a parent classloader or a non-decompiled jar.

## Open Questions
- Can we infer the game loop from `TaskRegistry`?
- How does `EventRegistry` connect to the missing `EventBus`?

## Next Actions
1.  Analyze `EventRegistry` (if present) to understand event flow.
2.  Proceed to Data Models to see if we can find Packet definitions (which might be in `common`).
