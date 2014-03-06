package org.projectodd.wunderboss;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Options extends HashMap<String, Object> {

    public Options() {
        this(null);
    }

    public Options(Map<String, Object> options) {
        if (options != null) {
            putAll(options);
        }
    }

    public Object get(String key, Object defaultValue) {
        return get(key) != null ? get(key) : defaultValue;
    }

    public Integer getInt(String key) {
        return getInt(key, null);
    }

    public Integer getInt(String key, Integer defaultValue) {
        Object value = get(key);
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return defaultValue;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        return get(key) != null ? get(key).toString() : defaultValue;
    }

    public Options put(String key, Object value) {
        super.put(key, value);

        return this;
    }

    public Options merge(Options otherOptions) {
        Options mergedOptions = new Options();
        for (String key : this.keySet()) {
            mergedOptions.put(key, this.get(key));
        }
        for (String key : otherOptions.keySet()) {
            mergedOptions.put(key, otherOptions.get(key));
        }
        return mergedOptions;
    }
}

