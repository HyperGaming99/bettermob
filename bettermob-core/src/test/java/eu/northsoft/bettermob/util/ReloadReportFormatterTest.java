package eu.northsoft.bettermob.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadReportFormatterTest {

    @Test
    void formatsEmptyDiffReport() {
        ReloadReport report = new ReloadReport(0, Map.of(), 0);
        List<String> lines = ReloadReportFormatter.format(report, false, false);
        assertEquals(2, lines.size());
        assertEquals("§aBetterMob reloaded.", lines.get(0));
        assertEquals("§7No definitions changed.", lines.get(1));

        List<String> germanLines = ReloadReportFormatter.format(report, false, true);
        assertEquals(2, germanLines.size());
        assertEquals("§aBetterMob neu geladen.", germanLines.get(0));
        assertEquals("§7Keine Definition hat sich geändert.", germanLines.get(1));
    }

    @Test
    void formatsDetailedDiffReport() {
        Map<String, ReloadDiff> diffs = new LinkedHashMap<>();
        diffs.put("mobs", new ReloadDiff(List.of("boss1"), List.of("old_mob"), List.of("zombie")));
        ReloadReport report = new ReloadReport(2, diffs, 1);

        List<String> lines = ReloadReportFormatter.format(report, false, true);
        assertTrue(lines.contains("§aBetterMob neu geladen."));
        assertTrue(lines.contains("§7mobs: §a1 neu§7, §c1 entfernt§7, §e1 geändert"));
        assertTrue(lines.contains("§8  + §7boss1"));
        assertTrue(lines.contains("§8  - §7old_mob"));
        assertTrue(lines.contains("§8  ~ §7zombie"));
        assertTrue(lines.contains("§71 lebende Mobs nutzen eine geänderte Mob-Definition."));
        assertTrue(lines.contains("§72 lebende Mobs werden auf die neue Definition umgestellt."));
    }
}
