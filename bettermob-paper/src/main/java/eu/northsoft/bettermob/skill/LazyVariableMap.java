package eu.northsoft.bettermob.skill;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LazyVariableMap implements Map<String, String> {
    private volatile Map<String, String> delegate;

    private Map<String, String> ensureDelegate() {
        Map<String, String> map = delegate;
        if (map == null) {
            synchronized (this) {
                map = delegate;
                if (map == null) {
                    map = new ConcurrentHashMap<>(4);
                    delegate = map;
                }
            }
        }
        return map;
    }

    @Override
    public int size() {
        Map<String, String> map = delegate;
        return map != null ? map.size() : 0;
    }

    @Override
    public boolean isEmpty() {
        Map<String, String> map = delegate;
        return map == null || map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        Map<String, String> map = delegate;
        return map != null && map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        Map<String, String> map = delegate;
        return map != null && map.containsValue(value);
    }

    @Override
    public String get(Object key) {
        Map<String, String> map = delegate;
        return map != null ? map.get(key) : null;
    }

    @Override
    public String put(String key, String value) {
        return ensureDelegate().put(key, value);
    }

    @Override
    public String remove(Object key) {
        Map<String, String> map = delegate;
        return map != null ? map.remove(key) : null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        if (m != null && !m.isEmpty()) {
            ensureDelegate().putAll(m);
        }
    }

    @Override
    public void clear() {
        Map<String, String> map = delegate;
        if (map != null) map.clear();
    }

    @Override
    public Set<String> keySet() {
        Map<String, String> map = delegate;
        return map != null ? map.keySet() : Collections.emptySet();
    }

    @Override
    public Collection<String> values() {
        Map<String, String> map = delegate;
        return map != null ? map.values() : Collections.emptyList();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Map<String, String> map = delegate;
        return map != null ? map.entrySet() : Collections.emptySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map<?, ?> other)) return false;
        Map<String, String> map = delegate;
        if (map == null) return other.isEmpty();
        return map.equals(other);
    }

    @Override
    public int hashCode() {
        Map<String, String> map = delegate;
        return map != null ? map.hashCode() : 0;
    }
}
