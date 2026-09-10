package backend.storyweave;

import java.nio.file.Path;

final class ServerLoggerTest {
    static void run() {
        Path httpLog = ServerLogger.httpLogFile();
        Path llmLog = ServerLogger.llmLogFile();

        check(httpLog.getParent().equals(Path.of("logs", "http")), "HTTP logs should use their own directory");
        check(llmLog.getParent().equals(Path.of("logs", "llm")), "LLM logs should use their own directory");
        check(httpLog.getFileName().equals(llmLog.getFileName()), "one run should use matching log filenames");
        check(httpLog.getFileName().toString().matches("\\d{8}-\\d{6}-\\d{3}\\.log"),
                "run log filenames should contain a filesystem-safe timestamp");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}