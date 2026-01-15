# Reverse Engineering Knowledge Base - Source of Truth

**Status:** Active
**Last Updated:** 2026-01-15

## Repo Overview
This repository contains reverse-engineering artifacts for the Hytale game server (`decompiled/`), along with custom plugins (`meine_mods`) developed for it. The core analysis targets the `decompiled/` Java sources and the API usage within the plugins.

## Documentation Map
| Document | Scope | Status |
| :--- | :--- | :--- |
| [Repo Map](./repo-map.md) | Directory structure & entry points | **Done** |
| [Runtime Flow](./runtime-flow.md) | Execution flow, lifecycle, events | **Done** |
| [Data Models](./data-models.md) | Data structures, serialization, config | **Done** |
| [Interfaces](./interfaces.md) | APIs, Hooks, Network | **Done** |
| [Assets & Tooling](./assets-and-tooling.md) | Build pipelines, asset formats | **Done** |

## Glossary
* **Decompiled**: Refers to the code in `decompiled/`, acting as a partial SDK/API definition.
* **Meine Mods**: Custom plugins found in `meine_mods/`.
* **AssetPack**: A collection of assets (textures, models) bundled with a plugin.
* **PluginState**: The lifecycle state of a plugin (SETUP, START, ENABLED, etc.).
* **HytaleServer**: The core server class (referenced but missing from decompiled sources).

## Findings Log
| Date | Change/Finding | Author |
| :--- | :--- | :--- |
| 2026-01-15 | Initialized Knowledge Base | Jules |
| 2026-01-15 | Mapped Repo Structure & Entry Points | Worker A |
| 2026-01-15 | Analyzed Runtime Flow & Plugin Lifecycle | Worker B |
| 2026-01-15 | Documented Data Models & Serialization | Worker C (API) |
| 2026-01-15 | Documented Interfaces & Hooks | Worker D (API) |
| 2026-01-15 | Analyzed Asset Pack System | Worker E |

## Known Unknowns
* Exact version of the Hytale artifact in `decompiled/`.
* Location of the main game loop and network packet definitions (likely in non-decompiled parent classes).
* Internal format of Asset Pack resources (models/animations).

## Reverse Engineering Traceability Table
| Claim | Evidence | Doc Link | Confidence |
| :--- | :--- | :--- | :--- |
| Plugins extend `JavaPlugin` | `de.hazy.HazyPlugin` extends `JavaPlugin` | [Runtime Flow](./runtime-flow.md) | High |
| Server is Server-Authoritative | `deathmatchPlugin` design docs | [Data Models](./data-models.md) | Med |
| Plugins can include Assets | `manifest.json` "IncludesAssetPack" field | [Assets](./assets-and-tooling.md) | High |
| Config uses Codecs | `DuelConfig.java` usage | [Data Models](./data-models.md) | High |
| Networking uses Gson | `GeminiService.java` usage | [Data Models](./data-models.md) | High |
