package net.deinserver.livingorbis;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GeminiService
 * 
 * Lightweight integration for Google Gemini 2.0 Flash using native Java HttpClient.
 * Replaces the heavy Google GenAI SDK to avoid classpath conflicts (GRPC/Protobuf) on the Hytale server.
 */
public class GeminiService {

    private static final String MODEL_ID = "gemini-2.0-flash-lite";
    private static final String API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_ID + ":generateContent?key=";
    private static final Logger LOGGER = Logger.getLogger("LivingOrbis-Gemini");

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Sends a prompt to Gemini asynchronously.
     */
    public CompletableFuture<String> askGemini(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Construct JSON Payload: { "contents": [{ "parts": [{ "text": "..." }] }] }
                JsonObject part = new JsonObject();
                part.addProperty("text", prompt);

                JsonArray parts = new JsonArray();
                parts.add(part);

                JsonObject content = new JsonObject();
                content.addProperty("role", "user"); // REQUIRED by v1beta
                content.add("parts", parts);

                JsonArray contents = new JsonArray();
                contents.add(content);

                JsonObject requestBody = new JsonObject();
                requestBody.add("contents", contents);

                String jsonBody = gson.toJson(requestBody);

                // Build Request
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_ENDPOINT + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

                // Send Request
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                } else {
                    LOGGER.log(Level.WARNING, "Gemini API Error: " + response.statusCode() + " - " + response.body());
                    return "Der NPC ist verwirrt. (API Error " + response.statusCode() + ")";
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to call Gemini API", e);
                return "Der NPC ist sprachlos. (Internal Error)";
            }
        });
    }

    private String parseResponse(String jsonResponse) {
        try {
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    return parts.get(0).getAsJsonObject().get("text").getAsString();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse Gemini response", e);
        }
        return "Der NPC murmelt unverständliches Zeug.";
    }
}
