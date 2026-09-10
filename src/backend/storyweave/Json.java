package backend.storyweave;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON reader and writer used to keep the server dependency-free. */
public final class Json {
    private Json() {
    }

    public static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Unexpected content after JSON value");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String source) {
        Object value = parse(source);
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        return (Map<String, Object>) value;
    }

    public static String stringify(Object value) {
        StringBuilder result = new StringBuilder();
        write(value, result);
        return result.toString();
    }

    private static void write(Object value, StringBuilder result) {
        if (value == null) {
            result.append("null");
        } else if (value instanceof String text) {
            writeString(text, result);
        } else if (value instanceof Number || value instanceof Boolean) {
            result.append(value);
        } else if (value instanceof Map<?, ?> map) {
            result.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    result.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), result);
                result.append(':');
                write(entry.getValue(), result);
            }
            result.append('}');
        } else if (value instanceof Iterable<?> values) {
            result.append('[');
            boolean first = true;
            for (Object item : values) {
                if (!first) {
                    result.append(',');
                }
                first = false;
                write(item, result);
            }
            result.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
        }
    }

    private static void writeString(String value, StringBuilder result) {
        result.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        result.append('"');
    }

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source;
        }

        private Object readValue() {
            skipWhitespace();
            if (atEnd()) {
                throw error("Expected a JSON value");
            }
            return switch (source.charAt(position)) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            position++;
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                if (atEnd() || source.charAt(position) != '"') {
                    throw error("Expected an object key");
                }
                String key = readString();
                skipWhitespace();
                require(':');
                result.put(key, readValue());
                skipWhitespace();
                if (consume('}')) {
                    return result;
                }
                require(',');
            }
        }

        private List<Object> readArray() {
            ArrayList<Object> result = new ArrayList<>();
            position++;
            skipWhitespace();
            if (consume(']')) {
                return result;
            }
            while (true) {
                result.add(readValue());
                skipWhitespace();
                if (consume(']')) {
                    return result;
                }
                require(',');
            }
        }

        private String readString() {
            position++;
            StringBuilder result = new StringBuilder();
            while (!atEnd()) {
                char character = source.charAt(position++);
                if (character == '"') {
                    return result.toString();
                }
                if (character != '\\') {
                    result.append(character);
                    continue;
                }
                if (atEnd()) {
                    throw error("Incomplete string escape");
                }
                char escape = source.charAt(position++);
                switch (escape) {
                    case '"', '\\', '/' -> result.append(escape);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(readUnicode());
                    default -> throw error("Invalid string escape");
                }
            }
            throw error("Unterminated string");
        }

        private char readUnicode() {
            if (position + 4 > source.length()) {
                throw error("Incomplete Unicode escape");
            }
            try {
                char result = (char) Integer.parseInt(source.substring(position, position + 4), 16);
                position += 4;
                return result;
            } catch (NumberFormatException exception) {
                throw error("Invalid Unicode escape");
            }
        }

        private Object readNumber() {
            int start = position;
            if (consume('-')) {
                // Sign consumed.
            }
            readDigits();
            boolean decimal = false;
            if (consume('.')) {
                decimal = true;
                readDigits();
            }
            if (!atEnd() && (source.charAt(position) == 'e' || source.charAt(position) == 'E')) {
                decimal = true;
                position++;
                if (!atEnd() && (source.charAt(position) == '+' || source.charAt(position) == '-')) {
                    position++;
                }
                readDigits();
            }
            if (start == position) {
                throw error("Expected a number");
            }
            String number = source.substring(start, position);
            try {
                if (decimal) {
                    return Double.parseDouble(number);
                }
                return Long.parseLong(number);
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private void readDigits() {
            int start = position;
            while (!atEnd() && Character.isDigit(source.charAt(position))) {
                position++;
            }
            if (start == position) {
                throw error("Expected a digit");
            }
        }

        private Object readLiteral(String literal, Object value) {
            if (!source.startsWith(literal, position)) {
                throw error("Invalid literal");
            }
            position += literal.length();
            return value;
        }

        private boolean consume(char expected) {
            if (!atEnd() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void require(char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }

        private boolean atEnd() {
            return position >= source.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + position);
        }
    }
}