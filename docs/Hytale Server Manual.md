# Hytale Server Manual

*Last Updated: 2 days ago*

This article covers the setup, configuration, and operation of dedicated Hytale servers.

**Intended Audience:** Server administrators and players hosting dedicated servers.

## Contents

* [Server Setup](#server-setup) (Java installation, server files, system requirements)
* [Running a Hytale Server](#running-a-hytale-server) (Launch commands, authentication, ports, firewall, file structure)
* [Tips & Tricks](#tips--tricks) (Mods, AOT cache, Sentry, recommended plugins, view distance)
* [Multiserver Architecture](#multiserver-architecture) (Player referral, redirects, fallbacks, building proxies)
* [Misc Details](#misc-details) (JVM arguments, protocol updates, configuration files)
* [Future Additions](#future-additions) (Server discovery, parties, integrated payments, SRV records, API endpoints)

---

## Server Setup

The Hytale server can run on any device with at least **4GB of memory** and **Java 25**. Both x64 and arm64 architectures are supported.

We recommend monitoring RAM and CPU usage while the server is in use to understand typical consumption for your player count and playstyle - resource usage heavily depends on player behavior.

**General guidance:**

| Resource | Driver |
| :--- | :--- |
| **CPU** | High player or entity counts (NPCs, mobs) |
| **RAM** | Large loaded world area (high view distance, players exploring independently) |

> **Note:** Without specialized tooling, it can be hard to determine how much allocated RAM a Java process actually needs. Experiment with different values for Java's `-Xmx` parameter to set explicit limits. A typical symptom of memory pressure is increased CPU usage due to garbage collection.

### Installing Java 25

Install Java 25. We recommend **Adoptium**.

**Confirm Installation:**
Verify the installation by running:

```bash
java --version
```

**Expected output:**

```text
openjdk 25.0.1 2025-10-21 LTS
OpenJDK Runtime Environment Temurin-25.0.1+8 (build 25.0.1+8-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.1+8 (build 25.0.1+8-LTS, mixed mode, sharing)
```

### Server Files

Two options to obtain server files:

1. **Manually copy from your Launcher installation**
2. **Use the Hytale Downloader CLI**

#### 1. Manually Copy from Launcher

*Best for: Quick testing. Annoying to keep updated.*

Find the files in your launcher installation folder:

* **Windows:** `%appdata%\Hytale\install\release\package\game\latest`
* **Linux:** `$XDG_DATA_HOME/Hytale/install/release/package/game/latest`
* **MacOS:** `~/Application Support/Hytale/install/release/package/game/latest`

List the content of the directory:

```bash
ls
```

**Expected output:**

```text
Directory: C:\Users\...\Hytale\install\release\package\game\latest

Mode                 LastWriteTime         Length Name
----                 -------------         ------ ----
d-----        12/25/2025   9:25 PM                Client
d-----        12/25/2025   9:25 PM                Server
-a----        12/25/2025   9:04 PM     3298097359 Assets.zip
```

Copy the `Server` folder and `Assets.zip` to your destination server folder.

#### 2. Hytale Downloader CLI

*Best for: Production servers. Easy to keep updated.*

A command-line tool to download Hytale server and asset files with OAuth2 authentication. See `QUICKSTART.md` inside the archive.

**Download:** `hytale-downloader.zip` (Linux & Windows)

| Command | Description |
| :--- | :--- |
| `./hytale-downloader` | Download latest release |
| `./hytale-downloader -print-version` | Show game version without downloading |
| `./hytale-downloader -version` | Show hytale-downloader version |
| `./hytale-downloader -check-update` | Check for hytale-downloader updates |
| `./hytale-downloader -download-path game.zip` | Download to specific file |
| `./hytale-downloader -patchline pre-release` | Download from pre-release channel |
| `./hytale-downloader -skip-update-check` | Skip automatic update check |

---

## Running a Hytale Server

Start the server with:

```bash
java -jar HytaleServer.jar --assets PathToAssets.zip
```

### Authentication

After first launch, authenticate your server.

```text
> /auth login device
===================================================================
DEVICE AUTHORIZATION
===================================================================
Visit: https://accounts.hytale.com/device
Enter code: ABCD-1234
Or visit: https://accounts.hytale.com/device?user_code=ABCD-1234
===================================================================
Waiting for authorization (expires in 900 seconds)...

[User completes authorization in browser]

> Authentication successful! Mode: OAUTH_DEVICE
```

Once authenticated, your server can accept player connections.

#### Additional Authentication Information

Hytale Servers require authentication to enable communication with our service APIs and to counter abuse.

> **Note:** There is a limit of **100 servers per Hytale game license** to prevent early abuse. If you need more capacity, purchase additional licenses or apply for a Server Provider account.

If you need to authenticate a large amount of servers or dynamically authenticate servers automatically, please read the **Server Provider Authentication Guide** for detailed information.

### Help

Review all available arguments:

```bash
java -jar HytaleServer.jar --help
```

**Expected output:**

```text
Option                                   Description
------                                   -----------
--accept-early-plugins                   Acknowledge that loading early plugins
                                           is unsupported and may cause stability issues
--allow-op
--assets <Path>                          Asset directory (default: ..\HytaleAssets)
--auth-mode <authenticated|offline>      Authentication mode (default: AUTHENTICATED)
-b, --bind <InetSocketAddress>           Address to listen on (default: 0.0.0.0:5520)
--backup                                 Enable automatic backups
--backup-dir <Path>                      Backup directory
--backup-frequency <Integer>             Backup interval in minutes (default: 30)
[...]
```

### Port

Default port is **5520**. Change it with the `--bind` argument:

```bash
java -jar HytaleServer.jar --assets PathToAssets.zip --bind 0.0.0.0:25565
```

### Firewall & Network Configuration

Hytale uses the **QUIC protocol over UDP** (not TCP). Configure your firewall and port forwarding accordingly.

#### Port Forwarding

If hosting behind a router, forward **UDP port 5520** (or your custom port) to your server machine. TCP forwarding is not required.

#### Firewall Rules

**Windows Defender Firewall:**

```powershell
New-NetFirewallRule -DisplayName "Hytale Server" -Direction Inbound -Protocol UDP -LocalPort 5520 -Action Allow
```

**Linux (iptables):**

```bash
sudo iptables -A INPUT -p udp --dport 5520 -j ACCEPT
```

**Linux (ufw):**

```bash
sudo ufw allow 5520/udp
```

#### NAT Considerations

QUIC handles NAT traversal well in most cases. If players have trouble connecting:

* Ensure the port forward is specifically for **UDP**, not TCP.
* Symmetric NAT configurations may cause issues - consider a VPS or dedicated server.
* Players behind carrier-grade NAT (common on mobile networks) should connect fine as clients.

### File Structure

| Path | Description |
| :--- | :--- |
| `.cache/` | Cache for optimized files |
| `logs/` | Server log files |
| `mods/` | Installed mods |
| `universe/` | World and player save data |
| `bans.json` | Banned players |
| `config.json` | Server configuration |
| `permissions.json` | Permission configuration |
| `whitelist.json` | Whitelisted players |

#### Universe Structure

The `universe/worlds/` directory contains all playable worlds. Each world has its own `config.json`:

```json
{
  "Version": 4,
  "UUID": {
    "$binary": "j2x/idwTQpen24CDfH1+OQ==",
    "$type": "04"
  },
  "Seed": 1767292261384,
  "WorldGen": {
    "Type": "Hytale",
    "Name": "Default"
  },
  "WorldMap": {
    "Type": "WorldGen"
  },
  "ChunkStorage": {
    "Type": "Hytale"
  },
  "ChunkConfig": {},
  "IsTicking": true,
  "IsBlockTicking": true,
  "IsPvpEnabled": false,
  "IsFallDamageEnabled": true,
  "IsGameTimePaused": false,
  "GameTime": "0001-01-01T08:26:59.761606129Z",
  "RequiredPlugins": {},
  "IsSpawningNPC": true,
  "IsSpawnMarkersEnabled": true,
  "IsAllNPCFrozen": false,
  "GameplayConfig": "Default",
  "IsCompassUpdating": true,
  "IsSavingPlayers": true,
  "IsSavingChunks": true,
  "IsUnloadingChunks": true,
  "IsObjectiveMarkersEnabled": true,
  "DeleteOnUniverseStart": false,
  "DeleteOnRemove": false,
  "ResourceStorage": {
    "Type": "Hytale"
  },
  "Plugin": {}
}
```

Each world runs on its own main thread and off-loads parallel work into a shared thread pool.

---

## Tips & Tricks

### Installing Mods

Download mods (`.zip` or `.jar`) from sources like CurseForge and drop them into the `mods/` folder.

### Disable Sentry Crash Reporting

**Important:** Disable Sentry during active plugin development.
We use Sentry to track crashes. Disable it with `--disable-sentry` to avoid submitting your development errors:

```bash
java -jar HytaleServer.jar --assets PathToAssets.zip --disable-sentry
```

### Leverage Ahead-Of-Time Cache

The server ships with a pre-trained AOT cache (`HytaleServer.aot`) that improves boot times by skipping JIT warmup. See JEP-514.

```bash
java -XX:AOTCache=HytaleServer.aot -jar HytaleServer.jar --assets PathToAssets.zip
```

### Recommended Plugins

Our development partners at Nitrado and Apex Hosting maintain plugins for common server hosting needs:

| Plugin | Description |
| :--- | :--- |
| **Nitrado:WebServer** | Base plugin for web applications and APIs |
| **Nitrado:Query** | Exposes server status (player counts, etc.) via HTTP |
| **Nitrado:PerformanceSaver** | Dynamically limits view distance based on resource usage |
| **ApexHosting:PrometheusExporter** | Exposes detailed server and JVM metrics |

### View Distance

View distance is the main driver for RAM usage. We recommend limiting maximum view distance to **12 chunks (384 blocks)** for both performance and gameplay.

> For comparison: Minecraft servers default to 10 chunks (160 blocks). Hytale's default of 384 blocks is roughly equivalent to 24 Minecraft chunks. Expect higher RAM usage with default settings - tune this value based on your expected player count.

---

## Multiserver Architecture

Hytale supports native mechanisms for routing players between servers. No reverse proxy like BungeeCord is required.

### Player Referral

Transfers a connected player to another server. The server sends a referral packet containing the target host, port, and an optional 4KB payload. The client opens a new connection to the target and presents the payload during handshake.

```java
PlayerRef.referToServer(@Nonnull final String host, final int port, @Nullable byte[] data)
```

> **⚠️ Security Warning:** The payload is transmitted through the client and can be tampered with. Sign payloads cryptographically (e.g., HMAC with a shared secret) so the receiving server can verify authenticity.

**Use cases:** Transferring players between game servers, passing session context, gating access behind matchmaking.

*Coming Soon: Array of targets tried in sequence for fallback connections.*

### Connection Redirect

During connection handshake, a server can reject the player and redirect them to a different server. The client automatically connects to the redirected address.

```java
PlayerSetupConnectEvent.referToServer(@Nonnull final String host, final int port, @Nullable byte[] data)
```

**Use cases:** Load balancing, regional server routing, enforcing lobby-first connections.

### Disconnect Fallback

When a player is unexpectedly disconnected (server crash, network interruption), the client automatically reconnects to a pre-configured fallback server instead of returning to the main menu.

**Use cases:** Returning players to a lobby after game server crash, maintaining engagement during restarts.

*Coming Soon: Fallback packet implementation expected within weeks after Early Access launch.*

### Building a Proxy

Build custom proxy servers using **Netty QUIC**. Hytale uses QUIC exclusively for client-server communication.

Packet definitions and protocol structure are available in `HytaleServer.jar`:
`com.hypixel.hytale.protocol.packets`

Use these to decode, inspect, modify, or forward traffic between clients and backend servers.

---

## Misc Details

### Java Command-Line Arguments

See Guide to the Most Important JVM Parameters for topics like `-Xms` and `-Xmx` to control heap size.

### Protocol Updates

The Hytale protocol uses a hash to verify client-server compatibility. If hashes don't match exactly, the connection is rejected.

**Current Limitation:** Client and server must be on the exact same protocol version. When we release an update, servers must update immediately or players on the new version cannot connect.

*Coming Soon: Protocol tolerance allowing ±2 version difference between client and server. Server operators will have a window to update without losing player connectivity.*

### Configuration Files

Configuration files (`config.json`, `permissions.json`, etc.) are read on server startup and written to when in-game actions occur (e.g., assigning permissions via commands). Manual changes while the server is running are likely to be overwritten.

### Maven Central Artifact

The `HytaleServer` jar will be published to Maven Central for use as a dependency in modding projects.

```xml
<dependency>
    <groupId>com.hypixel.hytale</groupId>
    <artifactId>Server</artifactId>
</dependency>
```

Exact details including versioning are pending for launch. Check modding community resources for the latest information on using these dependencies.

---

## Future Additions

### Server & Minigame Discovery

A discovery catalogue accessible from the main menu where players can browse and find servers and minigames. Server operators can opt into the catalogue to promote their content directly to players.

**Requirements for listing:**

| Requirement | Description |
| :--- | :--- |
| **Server Operator Guidelines** | Servers must adhere to operator guidelines and community standards |
| **Self-Rating** | Operators must accurately rate their server content. Ratings power content filtering and parental controls |
| **Enforcement** | Servers violating their self-rating are subject to enforcement actions per server operator policies |

**Player Count Verification:**
Player counts displayed in server discovery are gathered from client telemetry rather than server-reported data. This prevents count spoofing and ensures players can trust the numbers they see when browsing servers. Servers will still be able to report an unverified player count to users, who added the server outside of server discovery.

### Parties

A party system enabling players to group up and stay together across server transfers and minigame queues.

**Integration with server discovery:**
Players can browse servers with their party and join together. Party size requirements and restrictions are visible before joining, so groups know upfront if they can play together.

This system provides the foundation for a seamless social experience where friends can move through the Hytale ecosystem as a group without manual coordination.

### Integrated Payment System

A payment gateway built into the client that servers can use to accept payments from players. Optional but encouraged.

**Benefits for server operators:**

* Accept payments without handling payment details or building infrastructure
* Transactions processed through a trusted, secure system

**Benefits for players:**

* No need to visit external websites
* All transactions are secure and traceable
* Payment information stays within the Hytale ecosystem

### SRV Record Support

SRV records allow players to connect using a domain name (e.g., `play.example.com`) without specifying a port, with DNS handling the lookup to resolve the actual server address and port.

**Current Status: Unsupported. Under evaluation.**

**Why it's not available yet:**
There is no battle-tested C# library for SRV record resolution. Existing options either require pulling in a full DNS client implementation, which introduces unnecessary complexity and potential stability risks, or lack the production readiness we require for a core networking feature.

We are evaluating alternatives and will revisit this when a suitable solution exists.

### First-Party API Endpoints

Authenticated servers will have access to official API endpoints for player data, versioning, and server operations. These endpoints reduce the need for third-party services and provide authoritative data directly from Hytale.

**Planned Endpoints:**

| Endpoint | Description |
| :--- | :--- |
| **UUID ↔ Name Lookup** | Resolve player names to UUIDs and vice versa. Supports single and bulk lookups |
| **Game Version** | Query current game version, protocol version, and check for updates |
| **Player Profile** | Fetch player profile data including cosmetics, avatar renders, and public profile information |
| **Server Telemetry** | Report server status, player count, and metadata for discovery integration |
| **Report** | Report players for ToS violations |
| **Payments** | Process payments using our built-in payment gate |

**Under Consideration:**

| Endpoint | Description |
| :--- | :--- |
| **Global Sanctions** | Query whether a player has platform-level sanctions (not server-specific bans) |
| **Friends List** | Retrieve a player's friends list (with appropriate permissions) for social features |
| **Webhook Subscriptions** | Subscribe to push notifications for events like player name changes or sanction updates |

**Design Goals:**

* **Generous rate limits:** Bulk endpoints and caching-friendly responses to support large networks
* **Authenticated access:** All endpoints require server authentication to prevent abuse
* **Versioned API:** Stable contracts with deprecation windows for breaking changes
