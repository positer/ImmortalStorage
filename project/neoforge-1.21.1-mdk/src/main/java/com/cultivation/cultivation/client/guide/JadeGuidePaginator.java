package com.cultivation.cultivation.client.guide;

import java.util.ArrayList;
import java.util.List;

public final class JadeGuidePaginator {
    private final JadeGuideTextMeasurer measurer;
    private final int width;
    private final int linesPerPage;

    public JadeGuidePaginator(JadeGuideTextMeasurer measurer, int width, int linesPerPage) {
        if (width < 1 || linesPerPage < 1) throw new IllegalArgumentException("Page dimensions must be positive");
        this.measurer = measurer;
        this.width = width;
        this.linesPerPage = linesPerPage;
    }

    public List<JadeGuidePage> paginate(String title, List<String> paragraphs) {
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < paragraphs.size(); index++) {
            if (index > 0 && !lines.isEmpty()) lines.add("");
            wrap(paragraphs.get(index), lines);
        }
        if (lines.isEmpty()) lines.add("");

        List<JadeGuidePage> pages = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += linesPerPage) {
            pages.add(new JadeGuidePage(title,
                    lines.subList(start, Math.min(start + linesPerPage, lines.size()))));
        }
        return List.copyOf(pages);
    }

    private void wrap(String paragraph, List<String> output) {
        String remaining = paragraph == null ? "" : paragraph.strip();
        if (remaining.isEmpty()) {
            output.add("");
            return;
        }
        while (!remaining.isEmpty()) {
            int end = fittingPrefix(remaining);
            if (end >= remaining.length()) {
                output.add(remaining);
                return;
            }
            int breakAt = lastWhitespace(remaining, end);
            if (breakAt <= 0) breakAt = end;
            String line = remaining.substring(0, breakAt).stripTrailing();
            if (line.isEmpty()) {
                int codePointEnd = remaining.offsetByCodePoints(0, 1);
                line = remaining.substring(0, codePointEnd);
                breakAt = codePointEnd;
            }
            output.add(line);
            remaining = remaining.substring(breakAt).stripLeading();
        }
    }

    private int fittingPrefix(String value) {
        int end = 0;
        while (end < value.length()) {
            int next = value.offsetByCodePoints(end, 1);
            if (measurer.width(value.substring(0, next)) > width) break;
            end = next;
        }
        return end == 0 ? value.offsetByCodePoints(0, 1) : end;
    }

    private static int lastWhitespace(String value, int before) {
        for (int index = before - 1; index >= 0; index--) {
            if (Character.isWhitespace(value.charAt(index))) return index;
        }
        return -1;
    }
}
