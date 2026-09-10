package backend.storygame;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Client for an OpenAI-compatible chat-completions endpoint. */
public final class RemoteStoryService implements StoryService {
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final HttpClient client;

    public RemoteStoryService(String endpoint, String apiKey, String model) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("The LLM URL is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("The API key is required");
        }
        this.endpoint = URI.create(endpoint);
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? "gpt-4o-mini" : model;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void verify() throws IOException, InterruptedException {
        complete("Reply with only OK.", 8);
    }

    @Override
    public String createStory(String theme, String playerName, int version, int playerCount) throws Exception {
        String prompt = PromptTemplates.load("story-generation.md", Map.of(
                "theme", theme,
                "playerName", playerName,
                "version", Integer.toString(version),
                "playerCount", Integer.toString(playerCount)
        ));
        return complete(prompt, 500).strip();
    }

    @Override
    public int scoreSimilarity(String sharedStory, String referenceStory) throws Exception {
        String prompt = PromptTemplates.load("similarity-score.md", Map.of(
                "sharedStory", sharedStory,
                "referenceStory", referenceStory
        ));
        return parseScore(complete(prompt, 20));
    }

    @Override
    public int scoreDeduction(String sharedStory, List<GameEngine.Entry> entries) throws Exception {
        String insertedTokens = entries.stream().map(GameEngine.Entry::token).toList().toString();
        String prompt = PromptTemplates.load("error-deduction.md", Map.of(
                "sharedStory", sharedStory,
                "insertedTokens", insertedTokens
        ));
        return parseScore(complete(prompt, 20));
    }

    private String complete(String prompt, int maxTokens) throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.4,
                "max_tokens", maxTokens,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(payload)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("LLM returned HTTP " + response.statusCode() + ": " + response.body());
        }
        try {
            Map<String, Object> body = Json.parseObject(response.body());
            List<?> choices = (List<?>) body.get("choices");
            Map<?, ?> firstChoice = (Map<?, ?>) choices.getFirst();
            Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");
            return String.valueOf(message.get("content"));
        } catch (RuntimeException exception) {
            throw new IOException("LLM response did not contain choices[0].message.content", exception);
        }
    }

    private static int parseScore(String response) throws IOException {
        String digits = response.replaceAll("[^0-9-]", "");
        try {
            return Math.clamp(Integer.parseInt(digits), 0, 100);
        } catch (NumberFormatException exception) {
            throw new IOException("LLM did not return an integer score: " + response, exception);
        }
    }
}