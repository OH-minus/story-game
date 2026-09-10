package backend.storyweave;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class PromptTemplates {
    private PromptTemplates() {
    }

    static String load(String fileName, Map<String, String> values) throws IOException {
        String resourceName = "/backend/prompts/" + fileName;
        String template;
        try (InputStream resource = PromptTemplates.class.getResourceAsStream(resourceName)) {
            if (resource == null) {
                throw new IOException("Prompt resource not found: " + resourceName);
            }
            template = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (Map.Entry<String, String> value : values.entrySet()) {
            template = template.replace("{{" + value.getKey() + "}}", value.getValue());
        }
        return template;
    }
}