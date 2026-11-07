package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.InlineSkills;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import eu.northsoft.bettermob.util.RegionEntities;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class EntitiesInRadiusTargeter implements Targeter {
    private final CandidateFilters filters;
    private final boolean playersOnly;
    private final int defaultLimit;
    private final boolean nearOrigin;
    private final boolean cone;

    public EntitiesInRadiusTargeter(CandidateFilters filters, boolean playersOnly, int defaultLimit, boolean nearOrigin) {
        this(filters, playersOnly, defaultLimit, nearOrigin, false);
    }

    public EntitiesInRadiusTargeter(CandidateFilters filters, boolean playersOnly, int defaultLimit, boolean nearOrigin, boolean cone) {
        this.filters = filters;
        this.playersOnly = playersOnly;
        this.defaultLimit = defaultLimit;
        this.nearOrigin = nearOrigin;
        this.cone = cone;
    }

    static boolean inCone(double yaw, double halfAngle, double dx, double dz) {
        if (dx * dx + dz * dz < 1.0e-9) return true;
        double toTarget = Math.toDegrees(Math.atan2(-dx, dz));
        double diff = Math.abs(((toTarget - yaw) % 360 + 540) % 360 - 180);
        return diff <= halfAngle;
    }

    private final Map<String, List<String>> conditionLines = new ConcurrentHashMap<>();

    private List<String> conditionLinesOf(String raw) {
        List<String> cached = conditionLines.get(raw);
        if (cached != null) return cached;
        if (conditionLines.size() >= 1024) {
            var it = conditionLines.keySet().iterator();
            for (int i = 0; i < 256 && it.hasNext(); i++) {
                it.next();
                it.remove();
            }
        }
        List<String> parsed = InlineSkills.splitInline(raw);
        conditionLines.put(raw, parsed);
        return parsed;
    }

    private record Hit(LivingEntity entity, double distance) {}

    @Override
    public List<Target> resolve(Map<String, String> params, SkillContext context) {
        int limit = parseInt(params.get("limit"), defaultLimit);
        if (limit <= 0) return List.of();

        Location center = nearOrigin && context.origin() != null ? context.origin() : context.caster().getLocation();
        double radius = parseFloat(firstParam(params, "r", "radius", "range"), cone ? 16f : playersOnly ? 10f : 5f);
        double halfAngle = parseFloat(firstParam(params, "a", "angle"), 90f) / 2.0;
        double yaw = center.getYaw() + parseFloat(firstParam(params, "rot", "rotation"), 0f);
        boolean targetPlayers = !"false".equalsIgnoreCase(firstParam(params, "targetplayers", "tp"));
        List<String> conditions = params.containsKey("conditions") ? conditionLinesOf(params.get("conditions")) : List.of();
        double radiusSquared = radius * radius;
        String sort = params.getOrDefault("sort", "nearest").toLowerCase(Locale.ROOT);

        if (limit == 1 && ("nearest".equals(sort) || "farthest".equals(sort))) {
            boolean findNearest = "nearest".equals(sort);
            LivingEntity best = null;
            double bestDist = findNearest ? Double.MAX_VALUE : -1.0;
            for (Entity entity : RegionEntities.near(center, radius)) {
                if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand || living.isDead()) continue;
                if (playersOnly && !(entity instanceof Player)) continue;
                Location loc = living.getLocation();
                if (cone && (living == context.caster() || (!targetPlayers && entity instanceof Player)
                        || !inCone(yaw, halfAngle, loc.getX() - center.getX(), loc.getZ() - center.getZ()))) continue;
                double distance = loc.distanceSquared(center);
                if (distance > radiusSquared) continue;
                if (findNearest ? distance < bestDist : distance > bestDist) {
                    if (filters.matches(living, conditions, context)) {
                        best = living;
                        bestDist = distance;
                    }
                }
            }
            return best != null ? List.of(Target.ofEntity(best)) : List.of();
        }

        List<Hit> hits = new ArrayList<>();
        for (Entity entity : RegionEntities.near(center, radius)) {
            if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand || living.isDead()) continue;
            if (playersOnly && !(entity instanceof Player)) continue;
            Location loc = living.getLocation();
            if (cone && (living == context.caster() || (!targetPlayers && entity instanceof Player)
                    || !inCone(yaw, halfAngle, loc.getX() - center.getX(), loc.getZ() - center.getZ()))) continue;
            double distance = loc.distanceSquared(center);
            if (distance > radiusSquared) continue;
            if (filters.matches(living, conditions, context)) hits.add(new Hit(living, distance));
        }
        switch (sort) {
            case "random" -> Collections.shuffle(hits);
            case "farthest" -> hits.sort(Comparator.comparingDouble(Hit::distance).reversed());
            default -> hits.sort(Comparator.comparingDouble(Hit::distance));
        }
        List<Target> targets = new ArrayList<>(Math.min(limit, hits.size()));
        for (Hit hit : hits) {
            if (targets.size() >= limit) break;
            targets.add(Target.ofEntity(hit.entity()));
        }
        return targets;
    }
}
