package com.immortalstorage.immortalstorage.client.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class JadeGuideSearch {
    private JadeGuideSearch() {
    }

    public static List<Result> search(List<Document> documents, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) return List.of();

        List<Result> matches = new ArrayList<>();
        for (Document document : documents) {
            String title = normalize(document.title());
            String body = normalize(document.body());
            String keywords = normalize(String.join(" ", document.keywords()));
            int score = score(title, body, keywords, normalizedQuery);
            if (score > 0) {
                matches.add(new Result(document.chapterId(), document.title(), document.body(),
                        document.unlocked(), score));
            }
        }
        matches.sort(Comparator.comparingInt(Result::score).reversed()
                .thenComparing(Result::title, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Result::chapterId));
        return List.copyOf(matches);
    }

    private static int score(String title, String body, String keywords, String query) {
        int score = 0;
        if (title.equals(query)) score += 100;
        if (title.startsWith(query)) score += 60;
        if (title.contains(query)) score += 40;
        if (keywords.contains(query)) score += 25;
        if (body.contains(query)) score += 10;
        if (score > 0) return score;

        String[] terms = query.split("\\s+");
        for (String term : terms) {
            if (term.isBlank()) continue;
            if (title.contains(term)) score += 10;
            else if (keywords.contains(term)) score += 6;
            else if (body.contains(term)) score += 2;
            else return 0;
        }
        return score;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }

    public record Document(String chapterId, String title, String body, List<String> keywords, boolean unlocked) {
        public Document {
            keywords = List.copyOf(keywords);
        }
    }

    public record Result(String chapterId, String title, String body, boolean unlocked, int score) {
    }
}
