package io.wunderboss;

import java.util.HashMap;
import java.util.Map;

public class Options {

    public Options() {
        this.options = new HashMap<>();
    }

    public Options(Map<String, Object> options) {
        this.options = options;
    }

    public Object get(String key) {
        return options.get(key);
    }

    public Object get(String key, Object defaultValue) {
        return options.get(key) != null ? options.get(key) : defaultValue;
    }

    public void put(String key, Object value) {
        options.put(key, value);
    }

    public boolean containsKey(String key) {
        return options.containsKey(key);
    }

    private Map<String, Object> options;
}
