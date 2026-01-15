package net.deinserver.livingorbis;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.logging.Level;

public class LivingOrbisPlugin extends JavaPlugin {
    
    private GeminiService geminiService;

    public LivingOrbisPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        getLogger().at(Level.INFO).log("Living Orbis (ECS Edition) wird initialisiert...");
        
        // TODO: Load API Key from config or environment
        String apiKey = "AIzaSyAFU5y5cJNBedem1mZScxwxE4tDojuHmiI";
        this.geminiService = new GeminiService(apiKey);

        // Register Command
        this.getCommandRegistry().registerCommand(new GeminiCommand(geminiService));
        
        getLogger().at(Level.INFO).log("Living Orbis Setup Complete. Use /ask to talk to Gemini.");
    }
}
