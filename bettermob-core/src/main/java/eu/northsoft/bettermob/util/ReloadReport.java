package eu.northsoft.bettermob.util;

import java.util.Map;

public record ReloadReport(int updated, Map<String, ReloadDiff> diffs, int affectedLiving) {}
