package backend.storyweave;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

final class ServerLogger {
    private static final Path LOG_DIRECTORY = Path.of("logs");
    private static final String RUN_TIMESTAMP = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now());
    private static final Path HTTP_LOG_FILE = LOG_DIRECTORY.resolve("http").resolve(RUN_TIMESTAMP + ".log");
    private static final Path LLM_LOG_FILE = LOG_DIRECTORY.resolve("llm").resolve(RUN_TIMESTAMP + ".log");

    private ServerLogger() {
    }

    static void logHttp(String method, String pathAndQuery, String requestBody, int status, String responseBody) {
        append(HTTP_LOG_FILE, "timestamp=" + Instant.now()
                + " method=" + safe(method)
                + " path=" + safe(pathAndQuery)
                + " request=" + quoted(requestBody)
                + " status=" + status
                + " response=" + quoted(responseBody));
    }

    static void logLlmRequest(String endpoint, String model, int maxTokens, String prompt) {
        append(LLM_LOG_FILE, "timestamp=" + Instant.now()
                + " event=request"
                + " endpoint=" + safe(endpoint)
                + " model=" + safe(model)
                + " maxTokens=" + maxTokens
                + " prompt=" + quoted(prompt));
    }

    static void logLlmResponse(String endpoint, int status, String responseBody) {
        append(LLM_LOG_FILE, "timestamp=" + Instant.now()
                + " event=response"
                + " endpoint=" + safe(endpoint)
                + " status=" + status
                + " body=" + quoted(responseBody));
    }

    static Path httpLogFile() {
        return HTTP_LOG_FILE;
    }

    static Path llmLogFile() {
        return LLM_LOG_FILE;
    }

    private static synchronized void append(Path file, String line) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.err.println("Logging failed for " + file + ": " + exception.getMessage());
        }
    }

    private static String quoted(String value) {
        return "'" + safe(value) + "'";
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("'", "''");
    }
}