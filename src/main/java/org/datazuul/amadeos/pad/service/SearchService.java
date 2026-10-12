package org.datazuul.amadeos.pad.service;

public final class SearchService {
    public int find(String text, String query, int from, boolean matchCase, boolean wrap) {
        if (query.isEmpty()) return -1;
        int index = search(text, query, from, matchCase);
        return index < 0 && wrap ? search(text, query, 0, matchCase) : index;
    }

    private int search(String text, String query, int from, boolean matchCase) {
        if (matchCase) return text.indexOf(query, from);
        // Case conversion can change string length; regionMatches preserves editor offsets.
        for (int i = Math.max(0, from); i <= text.length() - query.length(); i++) {
            if (text.regionMatches(true, i, query, 0, query.length())) return i;
        }
        return -1;
    }

    public boolean matches(String selected, String query, boolean matchCase) {
        return selected != null && !query.isEmpty()
                && (matchCase ? selected.equals(query) : selected.equalsIgnoreCase(query));
    }

    public Replacement replaceAll(String text, String query, String replacement, boolean matchCase) {
        if (query.isEmpty()) return new Replacement(text, 0);
        int count = 0;
        int position = 0;
        StringBuilder result = new StringBuilder();
        int index;
        while ((index = search(text, query, position, matchCase)) >= 0) {
            result.append(text, position, index).append(replacement);
            position = index + query.length();
            count++;
        }
        result.append(text, position, text.length());
        return new Replacement(result.toString(), count);
    }

    public record Replacement(String text, int count) {
    }
}
