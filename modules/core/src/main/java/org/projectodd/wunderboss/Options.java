package org.projectodd.wunderboss;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Options {

    public Options() {
        this(null);
    }

    public Options(Map<String, Object> options) {
        if (options != null) {
            this.options = options;
        } else {
            this.options = new HashMap<>();
        }
    }

    public Object get(String key) {
        return get(key, null);
    }

    public Object get(String key, Object defaultValue) {
        return options.get(key) != null ? options.get(key) : defaultValue;
    }

    public Integer getInt(String key) {
        return getInt(key, null);
    }

    public Integer getInt(String key, Integer defaultValue) {
        Object value = options.get(key);
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return defaultValue;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        return options.get(key) != null ? options.get(key).toString() : defaultValue;
    }

    public Options put(String key, Object value) {
        options.put(key, value);

        return this;
    }

    public boolean containsKey(String key) {
        return options.containsKey(key);
    }

    public Set<String> keySet() {
        return options.keySet();
    }

    public Options merge(Options otherOptions) {
        Options mergedOptions = new Options();
        for (String key : this.options.keySet()) {
            mergedOptions.put(key, this.options.get(key));
        }
        for (String key : otherOptions.options.keySet()) {
            mergedOptions.put(key, otherOptions.options.get(key));
        }
        return mergedOptions;
    }

    private Map<String, Object> options;
}
