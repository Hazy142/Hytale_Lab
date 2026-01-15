package de.hazy;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.logging.Level;
import javax.annotation.Nonnull;

/**
 * HazyPlugin - Hytale Server Plugin.
 * 
 * Registers the /hazy command which responds with "gang gang" in chat.
 */
public class HazyPlugin extends JavaPlugin {

    public HazyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("[hazyPlugin] Registering commands...");
        getCommandRegistry().registerCommand(new HazyCommand());
        getLogger().at(Level.INFO).log("[hazyPlugin] /hazy command registered!");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("[hazyPlugin] Plugin disabled.");
    }
}
