package net.deinserver.livingorbis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GeminiService {
    private final String apiKey;
    private final HttpClient client;
    private final Gson gson;

    // Nutzung der v1beta API. Hinweis: In Produktion (2026) auf v1 stable prüfen.
    // Configurable model name could be added here.
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public GeminiService(String apiKey) {
        this.apiKey = apiKey;
        // Erstellen eines neuen HttpClient.
        // In Java 25 nutzt dieser per Default einen effizienten Executor.
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    // Die Signatur signalisiert Asynchronität: CompletableFuture
    public CompletableFuture<String> askGemini(String prompt) {
        // 1. Aufbau des Request-Body (JSON Construction)
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        parts.add(textPart);

        JsonObject userRole = new JsonObject();
        userRole.addProperty("role", "user");
        userRole.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(userRole);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contents);

        // 2. Erstellen des HTTP Requests
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        // 3. Senden (Non-Blocking) und Transformieren der Antwort
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // HTTP Status Code Check
                    if (response.statusCode() != 200) {
                        // Logging des Fehlers wäre hier wichtig für Debugging
                        // System.err.println("API Error: " + response.body());
                        return "Der NPC schaut dich verwirrt an. (API Error: " + response.statusCode() + ")";
                    }
                    return extractTextFromResponse(response.body());
                });
    }

    // Hilfsmethode zum Parsen der verschachtelten Antwortstruktur
    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);
            // Navigation durch: candidates -> -> content -> parts -> -> text
            return root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            // Robustheit: Falls die API eine unerwartete Struktur sendet (z.B. Safety
            // Filter Block)
            return "... (Der NPC murmelt unverständlich)";
        }
    }
}
