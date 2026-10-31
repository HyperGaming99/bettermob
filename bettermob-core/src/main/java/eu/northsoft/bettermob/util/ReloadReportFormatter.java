package eu.northsoft.bettermob.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReloadReportFormatter {
    private static final int DIFF_IDS_SHOWN = 5;

    private ReloadReportFormatter() {}

    public static List<String> format(ReloadReport report, boolean verbose) {
        return format(report, verbose, isGerman());
    }

    public static List<String> format(ReloadReport report, boolean verbose, boolean german) {
        List<String> lines = new ArrayList<>();
        lines.add(german ? "§aBetterMob neu geladen." : "§aBetterMob reloaded.");

        boolean any = false;
        if (report != null && report.diffs() != null) {
            for (Map.Entry<String, ReloadDiff> entry : report.diffs().entrySet()) {
                ReloadDiff diff = entry.getValue();
                if (diff == null || diff.isEmpty()) continue;
                any = true;
                if (german) {
                    lines.add("§7" + entry.getKey() + ": §a" + diff.added().size() + " neu§7, §c"
                            + diff.removed().size() + " entfernt§7, §e" + diff.changed().size() + " geändert");
                } else {
                    lines.add("§7" + entry.getKey() + ": §a" + diff.added().size() + " new§7, §c"
                            + diff.removed().size() + " removed§7, §e" + diff.changed().size() + " changed");
                }
                addDiffIds(lines, "+", diff.added(), verbose);
                addDiffIds(lines, "-", diff.removed(), verbose);
                addDiffIds(lines, "~", diff.changed(), verbose);
            }
        }

        if (!any) {
            lines.add(german ? "§7Keine Definition hat sich geändert." : "§7No definitions changed.");
        }
        if (report != null) {
            if (report.affectedLiving() > 0) {
                lines.add(german ? "§7" + report.affectedLiving() + " lebende Mobs nutzen eine geänderte Mob-Definition."
                        : "§7" + report.affectedLiving() + " living mob(s) use a changed mob definition.");
            }
            if (report.updated() > 0) {
                lines.add(german ? "§7" + report.updated() + " lebende Mobs werden auf die neue Definition umgestellt."
                        : "§7" + report.updated() + " living mob(s) queued to update to their new definition.");
            }
        }

        return lines;
    }

    private static void addDiffIds(List<String> lines, String sign, List<String> ids, boolean verbose) {
        if (ids == null || ids.isEmpty()) return;
        List<String> shown = verbose || ids.size() <= DIFF_IDS_SHOWN ? ids : ids.subList(0, DIFF_IDS_SHOWN);
        String more = shown.size() < ids.size() ? " (+" + (ids.size() - shown.size()) + ")" : "";
        lines.add("§8  " + sign + " §7" + String.join(", ", shown) + more);
    }

    private static boolean isGerman() {
        try {
            return java.util.Locale.getDefault().getLanguage().equalsIgnoreCase("de");
        } catch (Throwable ignored) {
            return false;
        }
    }
}
