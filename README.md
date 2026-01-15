# Hytale Plugin Development Guide

This documentation provides a standardized approach for creating Hytale Server plugins using Java 25.

## 1. Prerequisites

* **Java Development Kit (JDK):** Version 25 (e.g., Eclipse Adoptium Temurin 25).
* **Hytale Server:** Unpacked server files (specifically `HytaleServer.jar`).

## 2. Project Structure

A clean, standard directory layout is recommended:

```text
myPlugin/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/myplugin/
│   │   │       ├── MyPlugin.java       # Main entry point
│   │   │       └── MyCommand.java      # Example command
│   │   └── resources/
│   │       └── manifest.json           # Plugin metadata (CRITICAL)
├── build/                              # Output directory (created automatically)
└── build_plugin.ps1                    # Build script
```

## 3. Essential Files

### `manifest.json`

**IMPORTANT:** Keys must be **PascalCase** (e.g., `Name`, `Version`), NOT camelCase.

```json
{
    "Name": "MyPlugin",
    "Group": "com.example",
    "Version": "1.0.0",
    "Main": "com.example.myplugin.MyPlugin",
    "Description": "A template Hytale plugin"
}
```

### Main Class (`MyPlugin.java`)

```java
package com.example.myplugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class MyPlugin extends JavaPlugin {
    public MyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        // Register commands and listeners here
        getCommandRegistry().registerCommand(new MyCommand());
    }
}
```

## 4. Build Process (Manual/Scripted)

Due to unstable Gradle/Maven support for Java 25 (preview features), a direct `javac` compilation is currently most reliable.

**Build Command (PowerShell):**

```powershell
# 1. Setup Environment
$SERVER_PATH = "..\Path\To\Server" # Adjust this path!
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 2. Compile
# Note: Adding HytaleServer.jar to classpath (-cp)
javac -cp "$SERVER_PATH\HytaleServer.jar" -d "build\classes" "src\main\java\com\example\myplugin\*.java"

# 3. Package
# Copy manifest to classes dir to include it in JAR
Copy-Item "src\main\resources\manifest.json" "build\classes\"
jar cf "build\MyPlugin-1.0.0.jar" -C "build\classes" .

# 4. Deploy
Copy-Item "build\MyPlugin-1.0.0.jar" "$SERVER_PATH\mods\" -Force
Write-Host "Build & Deploy Complete!"
```

## 5. Running the Server

To prevent "Out of Memory" crashes during world generation/startup, allocate sufficient RAM.

**Start Command:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
& "$env:JAVA_HOME\bin\java.exe" -Xmx4G -Xms2G -jar HytaleServer.jar --assets ..\Assets.zip --disable-sentry
```

* `-Xmx4G`: Maximum heap size (4GB).
* `--assets`: Path to `Assets.zip`.

## 6. Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| **Server Crash (OOM)** | Default Java heap is too small. | Use `-Xmx4G` or higher in start command. |
| **Plugin Not Loaded** | Manifest keys are lowercase. | Change `name` -> `Name`, `version` -> `Version` in `manifest.json`. |
| **Method Not Found** | API mismatch. | Ensure you are linking against the exact `HytaleServer.jar` you are running. |
| **Auth Error** | No token. | Run `/auth login device` in server console. |

## 7. API Internals (Decompiled Findings)

Key classes and patterns discovered from `HytaleServer.jar`:

### Core Classes

* **`com.hypixel.hytale.server.core.plugin.PluginBase`**: The abstract base for all plugins.
  * `getCommandRegistry()`: Access to register commands.
  * `setup()`, `start()`, `shutdown()`: Lifecycle methods.
* **`com.hypixel.hytale.server.core.plugin.JavaPlugin`**: Extends `PluginBase` for Java-based plugins.

### Commands

* **`com.hypixel.hytale.server.core.command.system.AbstractCommand`**: Base class for commands.
  * Constructor: `super(name, description, ...)`
  * `execute(CommandContext context)`: Method called when command is run.
* **`com.hypixel.hytale.server.core.command.system.CommandContext`**:
  * `context.sender()`: Returns `CommandSender`.
* **`com.hypixel.hytale.server.core.command.system.CommandSender`**:
  * `sendMessage(Message message)`: Sends chat to the user.

### Utilities

* **`com.hypixel.hytale.logger.HytaleLogger`**: Uses Flogger-style API.
  * `logger.at(Level.INFO).log("Message")`
* **`com.hypixel.hytale.common.text.Message`**: Factory for chat messages.
  * `Message.raw("Text")`: Simple string message.

### Asset/Manifest System

* **`com.hypixel.hytale.common.plugin.PluginManifest`**:
  * Uses `KeyedCodec` which enforces **PascalCase** keys (`Name`, `Version`, `Main`, `Group`).
  * Validation fails if keys are missing or null.
