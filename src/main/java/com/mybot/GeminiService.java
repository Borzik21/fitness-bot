package com.mybot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeminiService {

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiService() {
        this.apiKey = System.getenv("GEMINI_API_KEY");
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Переменная окружения 'GEMINI_API_KEY' не установлена.");
        }
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Получает предсказание от Gemini, используя всю историю диалога в качестве контекста.
     * @param history Список сообщений (контекст диалога).
     * @return Ответ от модели.
     */
    public String getSupport(List<Message> history) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + this.apiKey;

            List<Map<String, Object>> contents = new ArrayList<>();
            for (Message message : history) {
                Map<String, String> part = Map.of("text", message.text());
                // Gemini API ожидает, что роль пользователя будет "user", а модели - "model".
                String role = "user".equals(message.role()) ? "user" : "model";
                Map<String, Object> content = Map.of("role", role, "parts", List.of(part));
                contents.add(content);
            }
            Map<String, List<Map<String, Object>>> requestBodyMap = Map.of("contents", contents);
            String jsonBody = gson.toJson(requestBodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Ошибка от API Gemini: " + response.body());
                return "Абонемент просрочен. Код: " + response.statusCode();
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "Я что-то без сил... Спроси позже.";
        }
    }

    private String parseResponse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!root.has("candidates") || root.get("candidates").getAsJsonArray().isEmpty()) {
                System.err.println("Ответ от Gemini не содержит кандидатов (возможно, сработал safety filter): " + responseBody);
                return "Таймаут... кажется я перетренировался, пойду подышу";
            }
            return root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            System.err.println("Не удалось разобрать ответ от Gemini: " + responseBody);
            e.printStackTrace();
            return "Мне нужна передышка, давай поговорим позже";
        }
    }
}