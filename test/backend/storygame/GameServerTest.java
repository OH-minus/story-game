package backend.storygame;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

final class GameServerTest {
    static void run() throws Exception {
        GameEngine game = new GameEngine(2, 20, 30, 5);
        try (GameServer server = new GameServer(0, game, new LocalStoryService(), "lost cities", 2)) {
            server.start();
            String base = "http://localhost:" + server.port();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> health = get(client, base + "/api/health");
            check(health.statusCode() == 200, "health endpoint should respond");
            check(Json.parseObject(health.body()).get("status").equals("ok"), "health response should be JSON");

            HttpResponse<String> page = get(client, base + "/");
            check(page.statusCode() == 200 && page.body().contains("Storyweave"), "server should host the frontend");

            HttpResponse<String> badJoin = post(client, base + "/api/join", Map.of("name", "bad name"));
            check(badJoin.statusCode() == 400, "invalid names should be rejected before joining");
            HttpResponse<String> aliceJoin = post(client, base + "/api/join", Map.of("name", "Alice"));
            HttpResponse<String> bobJoin = post(client, base + "/api/join", Map.of("name", "Bob"));
            check(aliceJoin.statusCode() == 201 && bobJoin.statusCode() == 201, "two players should join");
            String aliceId = String.valueOf(Json.parseObject(aliceJoin.body()).get("playerId"));
            HttpResponse<String> state = get(client, base + "/api/state?playerId=" + aliceId);
            check(state.statusCode() == 200, "a joined player should retrieve synchronized state");
            check(Json.parseObject(state.body()).get("phase").equals("READING"), "full lobby should enter reading phase");
            check(get(client, base + "/api/state?playerId=unknown").statusCode() == 404,
                    "unknown players should not read game state");
        }
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String url, Object body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(body)))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}