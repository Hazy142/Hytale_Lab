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
    public void onEnable() {
        Logger logger = getLogger();
        logger.info("Living Orbis wird initialisiert... 🌍✨");

        // In einer Produktionsumgebung sollte dieser Key niemals im Code stehen.
        // Best Practice: Laden aus Umgebungsvariablen oder einer config.yml.
        // Für dieses Beispiel implementieren wir den Key direkt, wie angefordert.
        String apiKey = "YOUR-API-KEY";

        // Initialisierung des Service-Layers
        this.geminiService = new GeminiService(apiKey);

        // Register Command
        this.getCommandRegistry().registerCommand(new GeminiCommand(geminiService));
        
        getLogger().at(Level.INFO).log("Living Orbis Setup Complete. Use /ask to talk to Gemini.");
    }
}
