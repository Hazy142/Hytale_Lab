package net.deinserver.livingorbis;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.logging.Level;
import java.util.logging.Logger;

// Pseudo-Manager-Klassen (werden später implementiert)
class CognitiveManager { public CognitiveManager(Object plugin) {} }
class BehaviorManager { public BehaviorManager(Object plugin) {} }
class GameModeManager { public GameModeManager(Object plugin) {} }

public class LivingOrbisPlugin extends JavaPlugin {

    private GeminiService geminiService;
    private CognitiveManager cognitiveManager;
    private BehaviorManager behaviorManager;
    private GameModeManager gameModeManager;

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
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null) {
            apiKey = "YOUR-API-KEY";
        }

        // Initialisierung des Service-Layers
        this.geminiService = new GeminiService(apiKey);

        // Manager Initialisierung
        this.cognitiveManager = new CognitiveManager(this);
        this.behaviorManager = new BehaviorManager(this);
        this.gameModeManager = new GameModeManager(this);

        // Register Command
        this.getCommandRegistry().registerCommand(new GeminiCommand(geminiService));

        getLogger().at(Level.INFO).log("Living Orbis Setup Complete. Managers loaded.");
    }
}
