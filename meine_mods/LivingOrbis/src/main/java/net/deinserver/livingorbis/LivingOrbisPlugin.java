package net.deinserver.livingorbis;

import net.hytale.api.plugin.Plugin;
import net.hytale.api.HytaleServer;
import org.slf4j.Logger;

public class LivingOrbisPlugin extends Plugin {

    private GeminiService geminiService;

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

        // Registrierung des Event-Listeners beim Server-Manager
        // Dies verknüpft unsere Logik mit dem Game-Loop.
        HytaleServer.getEventManager().registerEvents(this, new NpcListener(geminiService));

        logger.info("Living Orbis ist bereit! Sprich mit den NPCs.");
    }

    @Override
    public void onDisable() {
        // Aufräumarbeiten. Der HttpClient (in Java 25) managt Ressourcen meist
        // automatisch,
        // aber explizites Schließen von Executors wäre hier guter Stil.
    }
}
