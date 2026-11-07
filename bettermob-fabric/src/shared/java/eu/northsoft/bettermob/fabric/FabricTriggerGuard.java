package eu.northsoft.bettermob.fabric;

import java.util.HashSet;
import java.util.Set;

final class FabricTriggerGuard {
    private final ThreadLocal<Set<String>> firing = ThreadLocal.withInitial(HashSet::new);

    boolean run(String key, Runnable action) {
        Set<String> active = firing.get();
        if (!active.add(key)) return false;
        try {
            action.run();
            return true;
        } finally {
            active.remove(key);
            if (active.isEmpty()) firing.remove();
        }
    }
}
