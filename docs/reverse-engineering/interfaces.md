# Public Interfaces and Plugin Hooks

## Scope

This document outlines the public-facing interfaces, plugin/mod hooks, and network boundaries of the Hytale server. The analysis is based on static analysis of decompiled server code and existing mods.

## Evidence

The following files were analyzed to produce this document:

- `decompiled/com/hypixel/hytale/common/plugin/PluginManifest.java`
- `decompiled/com/hypixel/hytale/server/core/plugin/JavaPlugin.java`
- `meine_mods/LivingOrbis/src/main/java/net/deinserver/livingorbis/LivingOrbisPlugin.java`
- `meine_mods/LivingOrbis/src/main/resources/manifest.json`
- `meine_mods/deathmatchPlugin/src/main/java/de/hazy/deathmatch/DeathmatchPlugin.java`
- `meine_mods/deathmatchPlugin/src/main/resources/manifest.json`
- `meine_mods/hazyPlugin/src/main/resources/manifest.json`

## Findings

The Hytale server exposes a plugin-based architecture for extending server functionality. The core components of this architecture are:

### 1. Plugin Manifest (`manifest.json`)

Each plugin must include a `manifest.json` file that describes the plugin's metadata. The manifest is deserialized into a `PluginManifest` object at runtime. Key fields include:

- `Name`: The name of the plugin.
- `Group`: The Java package group for the plugin.
- `Version`: The plugin's version.
- `Main`: The fully-qualified class name of the plugin's main class.
- `Description`: A human-readable description of the plugin.

### 2. Plugin Entry Point (`JavaPlugin`)

The `Main` class specified in the manifest must extend `com.hypixel.hytale.server.core.plugin.JavaPlugin`. This class serves as the entry point for the plugin and provides access to the server's core functionality.

The `JavaPlugin` class provides the following lifecycle methods:

- `onEnable()`: Called when the plugin is enabled.
- `setup()`: Called after `onEnable()`.
- `shutdown()`: Called when the plugin is disabled.

### 3. Command Registration

Plugins can register custom commands with the server using the `getCommandRegistry()` method. The `deathmatchPlugin` and `LivingOrbisPlugin` both demonstrate this functionality.

- **Example:** `getCommandRegistry().registerCommand(new MyCommand(...));`

### 4. Event Handling

The server uses an event-driven architecture. Plugins can register listeners for specific events using the `getEventRegistry()` method.

- **Example:** `getEventRegistry().registerGlobal(PlayerReadyEvent.class, playerEventHandler::onPlayerReady);`

This indicates a global event bus that plugins can subscribe to.

### 5. Entity Component System (ECS)

The `deathmatchPlugin` demonstrates that the server uses an Entity Component System. The `DamageEventHandler` is registered as a system with the `getEntityStoreRegistry()`.

- **Example:** `getEntityStoreRegistry().registerSystem(new DamageEventHandler(gameManager));`

This suggests that plugins can interact with the core game loop by creating and registering their own systems.

## Hypotheses

Based on the evidence, we can form the following hypotheses:

- **EventBus:** The server likely uses a central EventBus for dispatching events. The `registerGlobal` method suggests a pub/sub model. Further investigation is needed to identify all available event types.
- **Asset Pipeline:** The `PluginManifest.includesAssetPack` field suggests that plugins can include their own assets. The `AssetModule` appears to be responsible for loading and managing these assets.
- **Network Boundary:** While not directly observed in the analyzed files, the presence of `IMessageReceiver` in the decompiled code suggests a network layer that receives and processes messages. It is likely that plugins can register their own message handlers to extend the network protocol.

## Open Questions

- What other events are available on the EventBus?
- How does the asset pipeline work? Can plugins add new blocks, items, or entities?
- What is the full extent of the ECS? Can plugins create custom components and entities?
- Is it possible to register custom network message handlers?
- What APIs are available for interacting with the game world (e.g., creating/destroying blocks, spawning entities)?

## Next Actions
1. Search for a list of all `Event` subclasses to map the event surface.
2. Investigate `EntityStoreRegistry` to list available ECS components.
3. Determine how to instantiate new entities via API.
4. Verify if `IMessageReceiver` allows custom packet handling.
5. Create a "Hello World" network handler hypothesis test.
