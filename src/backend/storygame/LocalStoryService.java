package backend.storygame;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Predictable offline implementation for demos and development. */
public final class LocalStoryService implements StoryService {
    @Override
    public String createStory(String theme, String playerName, int version, int playerCount) {
        String direction = version % 2 == 0 ? "protect it together" : "race one another to claim it";
        return "In a world shaped by " + theme + ", Mira and Rowan discover a hidden map. "
                + "They decide to " + direction + ", unaware that the final mark points back to their own village. "
                + "At dawn, a difficult choice reveals which friendship can survive.";
    }

    @Override
    public int scoreSimilarity(String sharedStory, String referenceStory) {
        Set<String> sharedWords = words(sharedStory);
        Set<String> referenceWords = words(referenceStory);
        if (referenceWords.isEmpty()) {
            return 0;
        }
        int matches = 0;
        for (String word : sharedWords) {
            if (referenceWords.contains(word)) {
                matches++;
            }
        }
        return Math.clamp((int) Math.round(matches * 100.0 / referenceWords.size()), 0, 100);
    }

    @Override
    public int scoreDeduction(String sharedStory, List<GameEngine.Entry> entries) {
        int deduction = 0;
        for (int index = 1; index < entries.size(); index++) {
            if (isPunctuation(entries.get(index - 1).token()) && isPunctuation(entries.get(index).token())) {
                deduction += 2;
            }
        }
        return Math.min(deduction, 30);
    }

    private static Set<String> words(String text) {
        HashSet<String> result = new HashSet<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (!word.isBlank()) {
                result.add(word);
            }
        }
        return result;
    }

    private static boolean isPunctuation(String token) {
        return token.codePoints().allMatch(codePoint -> {
            int type = Character.getType(codePoint);
            return type >= Character.CONNECTOR_PUNCTUATION && type <= Character.OTHER_PUNCTUATION;
        });
    }
}