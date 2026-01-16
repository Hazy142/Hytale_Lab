# Data Models and Serialization

## Scope

This document analyzes the key data structures, configuration formats, and serialization pathways within the Hytale server architecture. The analysis is based on the decompiled server binary and the `deathmatchPlugin` and `LivingOrbis` mods.

## Evidence

The findings in this document are based on the following sources:

*   `decompiled/`: The decompiled Hytale server JAR.
*   `meine_mods/deathmatchPlugin/`: A deathmatch plugin with a detailed design document.
*   `meine_mods/LivingOrbis/`: A plugin that integrates with an external AI service.
*   `meine_mods/hazyPlugin/`: A simple command plugin.

## Findings

### Core Architecture

The Hytale server is built on a "server-side first" philosophy, where the server has absolute authority over game state and assets. This is a significant departure from traditional Minecraft modding, which often relies on client-side modifications. The server streams necessary assets to the client, ensuring a consistent experience for all players.

### Configuration

Hytale utilizes a codec-based system for configuration, which provides type-safe configuration loading and validation. This is a more robust approach than the YAML-based configuration used in Bukkit/Spigot. The `deathmatchPlugin`'s `DuelConfig.java` provides a clear example of this system.

### Serialization

The server uses Google's Gson library for JSON serialization and deserialization. This is evident in the `LivingOrbis` plugin's `GeminiService.java`, which uses Gson to construct and parse JSON requests to the Gemini API. The `deathmatchPlugin` also uses JSON for its `hytale.json` manifest file.

### Packets

While the exact structure of network packets is not explicitly detailed in the provided files, the `deathmatchPlugin`'s design document suggests a server-authoritative model with lag compensation. This implies that the server sends regular updates to the client to synchronize game state, and the client sends input to the server for processing.

## Hypotheses

*   The server uses a custom binary protocol for network communication, with JSON used for higher-level data interchange.
*   The server's Entity Component System (ECS) is the primary mechanism for managing game state and synchronizing it across the network.
*   The `hytale.json` manifest file is used by the server to load and initialize plugins, similar to Bukkit's `plugin.yml`.

## Open Questions

*   What is the exact structure of the network packets?
*   How does the server handle player authentication and session management?
*   What are the specifics of the server's lag compensation mechanism?
*   How are custom assets (models, textures, sounds) streamed to the client?

## Next Actions
1. Locate packet definitions in non-decompiled classes if possible.
2. Trace the usage of `IMessageReceiver` to find network entry points.
3. Investigate `AssetModule` for asset streaming logic.
4. Document the full schema of `manifest.json`.
5. Analyze `DuelConfig.java` to document the Codec system in detail.
