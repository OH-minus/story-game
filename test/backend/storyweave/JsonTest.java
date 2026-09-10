package backend.storyweave;

import java.util.List;
import java.util.Map;

final class JsonTest {
    static void run() {
        Map<String, Object> value = Json.parseObject("{\"name\":\"A\\nB\",\"count\":2,\"ratio\":1.5,\"ok\":true,\"items\":[null,\"x\"]}");
        check(value.get("name").equals("A\nB"), "escaped strings should parse");
        check(value.get("count").equals(2L), "integers should retain integer representation");
        check(value.get("ratio").equals(1.5), "decimal numbers should parse");
        check(value.get("items") instanceof List<?>, "arrays should parse");

        String encoded = Json.stringify(Map.of("quote", "\"\\\n", "values", List.of(1, true)));
        Map<String, Object> decoded = Json.parseObject(encoded);
        check(decoded.get("quote").equals("\"\\\n"), "JSON should round-trip escaped content");
        expectInvalid("{\"broken\":}");
        expectInvalid("[1] trailing");
    }

    private static void expectInvalid(String source) {
        try {
            Json.parse(source);
            throw new AssertionError("Expected invalid JSON to be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}