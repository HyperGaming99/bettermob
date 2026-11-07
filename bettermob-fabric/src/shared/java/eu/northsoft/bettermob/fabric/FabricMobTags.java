package eu.northsoft.bettermob.fabric;

import java.util.Collection;
import java.util.Locale;

public final class FabricMobTags {
    private static final String PREFIX = "bettermob.mob.";

    private FabricMobTags() {
    }

    public static String tag(String id) {
        String normalized = normalize(id);
        if (normalized == null) throw new IllegalArgumentException("Mob ID must not be blank or contain control characters");
        return PREFIX + normalized;
    }

    public static String mobId(Collection<String> tags) {
        if (tags == null) return null;
        String result = null;
        for (String tag : tags) {
            if (tag == null || !tag.startsWith(PREFIX)) continue;
            String id = normalize(tag.substring(PREFIX.length()));
            if (id != null && (result == null || id.compareTo(result) < 0)) {
                result = id;
            }
        }
        return result;
    }

    private static String normalize(String id) {
        if (id == null || id.isBlank() || id.codePoints().anyMatch(Character::isISOControl)) return null;
        return id.toLowerCase(Locale.ROOT);
    }
}
