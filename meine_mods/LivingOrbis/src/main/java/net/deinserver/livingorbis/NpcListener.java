package net.deinserver.livingorbis;

import net.hytale.api.event.Subscribe;
import net.hytale.api.event.entity.PlayerInteractEntityEvent;
import net.hytale.api.entity.NpcEntity;
import net.hytale.api.entity.Player;
import net.hytale.api.ChatColor;

public class NpcListener {

    private final GeminiService geminiService;

    public NpcListener(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @Subscribe
    public void onInteract(PlayerInteractEntityEvent event) {
        // 1. Validierung: Ist das Ziel ein NPC?
        if (!(event.getTarget() instanceof NpcEntity npc)) {
            return;
        }

        Player player = event.getPlayer();

        // 2. Feedback (UX): Sofortige Reaktion, um Latenz zu überbrücken
        player.sendMessage(ChatColor.GRAY + "Du sprichst " + npc.getName() + " an...");

        // 3. Kontext-Extraktion aus der Game-Engine
        String biome = npc.getLocation().getBiome().getName();
        String npcType = npc.getType().getName(); // z.B. "Kweebec", "Trork"
        String timeOfDay = npc.getWorld().getTime().isDay() ? "Tag" : "Nacht";

        // 4. Prompt Engineering (Java Text Blocks)
        String prompt = """
                Du bist ein NPC im Spiel Hytale.
                Rasse: %s
                Aktueller Ort: %s
                Tageszeit: %s

                Ein Spieler namens %s spricht dich an.
                Antworte in der Rolle dieses Charakters.
                Halte dich extrem kurz (maximal 2 Sätze).
                Sei geheimnisvoll oder hilfreich, je nach Rasse.
                """.formatted(npcType, biome, timeOfDay, player.getName());

        // 5. Asynchrone Ausführung
        geminiService.askGemini(prompt).thenAccept(response -> {
            // 6. Thread-Wechsel zurück zum Main-Thread
            player.getServer().getScheduler().runTask(() -> {
                // Finale Ausgabe der KI-Antwort
                player.sendMessage(ChatColor.YELLOW + npc.getName() + ": " + ChatColor.WHITE + response);

                // Erweiterungsidee: Sprechblasen-API nutzen
                // npc.showFloatingText(response, 5000);
            });
        });
    }
}
