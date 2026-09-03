package io.github.miklires.mreports.update;

import java.util.ArrayList;
import java.util.List;

final class SemanticVersion implements Comparable<SemanticVersion> {
    private final List<Integer> parts;
    private SemanticVersion(List<Integer> parts) { this.parts = parts; }
    static SemanticVersion parse(String value) {
        String normalized = value == null ? "" : value.strip().replaceFirst("^[vV]", "");
        String core = normalized.split("[-+]", 2)[0];
        String[] raw = core.split("\\.");
        if (raw.length == 0) throw new IllegalArgumentException("Empty version");
        List<Integer> parts = new ArrayList<>(raw.length);
        for (String part : raw) {
            if (!part.matches("\\d+")) throw new IllegalArgumentException("Invalid version: " + value);
            parts.add(Integer.parseInt(part));
        }
        return new SemanticVersion(List.copyOf(parts));
    }
    @Override public int compareTo(SemanticVersion other) {
        int length = Math.max(parts.size(), other.parts.size());
        for (int i = 0; i < length; i++) {
            int left = i < parts.size() ? parts.get(i) : 0;
            int right = i < other.parts.size() ? other.parts.get(i) : 0;
            int result = Integer.compare(left, right);
            if (result != 0) return result;
        }
        return 0;
    }
}
