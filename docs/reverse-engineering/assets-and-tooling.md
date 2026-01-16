# Assets, Content Pipeline, and Tooling

## Scope
- Analysis of asset management, loading, and distribution.
- Understanding the `AssetPack` system.

## Evidence
- `PluginManifest.java` contains `includesAssetPack` boolean field.
- `JavaPlugin.java` accesses `AssetModule.get()` and registers packs if `includesAssetPack` is true.
- No `assets` folder found in `meine_mods` sample plugins (they are code-only or metadata-only).
- `decompiled` code references `com.hypixel.hytale.assetstore.AssetPack`.

## Findings

### Asset Pack System
Hytale uses a concept of "Asset Packs" that can be embedded within plugins.
1.  **Declaration**: A plugin sets `"IncludesAssetPack": true` in its `manifest.json`.
2.  **Registration**: During `start0()`, `JavaPlugin` checks this flag.
3.  **Loading**: It calls `AssetModule.get().registerPack(id, this.file, this.getManifest())`.
    - `this.file` refers to the plugin jar/folder.
    - This implies the assets are located inside the plugin structure, likely under an `assets/` directory (standard convention), though not explicitly seen in the sample mods.

### Tooling
- **Gradle**: The primary build tool.
- **Manifest**: `manifest.json` is the central configuration file.

## Hypotheses
- The `AssetModule` handles streaming these assets to the client upon connection.
- Assets likely follow a specific directory structure (e.g., `assets/<namespace>/textures/...`) similar to Minecraft, but wrapped in this `AssetPack` object.

## Open Questions
- What is the internal file format of an Asset Pack?
- How are models defined? (JSON? proprietary?)

## Next Actions
- If an example asset pack becomes available, analyze its structure.
