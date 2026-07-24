package com.careerfit.common.logging;

import java.util.List;
import java.util.regex.Pattern;

public final class SensitiveLogSanitizer {

    private static final String SENSITIVE_KEY =
            "password|passwd|token|authorization|api[-_]?key|secret|documentText|jobPostingText|prompt|requestBody|responseBody";
    private static final String MASK = "***";

    private static final List<Replacement> REPLACEMENTS = List.of(
            new Replacement(
                    Pattern.compile("(?i)(\\bAuthorization\\b\\s*[:=]\\s*)(?:Bearer\\s+)?[^\\s,;]+"),
                    "$1" + MASK),
            new Replacement(
                    Pattern.compile("(?i)(\\\"(?:" + SENSITIVE_KEY + ")\\\"\\s*:\\s*\\\")(.*?)(\\\")"),
                    "$1" + MASK + "$3"),
            new Replacement(
                    Pattern.compile("(?i)(\\b(?:" + SENSITIVE_KEY
                            + ")\\b\\s*[=:]\\s*)(?:\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;]+)"),
                    "$1" + MASK),
            new Replacement(
                    Pattern.compile("(?i)(\\bBearer\\s+)[A-Za-z0-9._~+/=-]+"),
                    "$1" + MASK));

    private SensitiveLogSanitizer() {
    }

    public static String sanitize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String sanitized = message;
        for (Replacement replacement : REPLACEMENTS) {
            sanitized = replacement.pattern().matcher(sanitized).replaceAll(replacement.replacement());
        }
        return sanitized;
    }

    private record Replacement(Pattern pattern, String replacement) {
    }
}
