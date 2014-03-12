package org.projectodd.wunderboss;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Options<T> extends HashMap<T, Object> {

    public Options() {
        this(null);
    }

    public Options(Map<T, Object> options) {
        if (options != null) {
            putAll(options);
        }
    }

    public Object get(T key, Object defaultValue) {
        return get(key) != null ? get(key) : defaultValue;
    }

    public Integer getInt(T key) {
        return getInt(key, null);
    }

    public Integer getInt(T key, Integer defaultValue) {
        Object value = get(key);
        if (value != null) {
            return Integer.parseInt(value.toString());
        }
        return defaultValue;
    }

    public String getString(T key) {
        return getString(key, null);
    }

    public String getString(T key, String defaultValue) {
        return get(key) != null ? get(key).toString() : defaultValue;
    }

    public Date getDate(T key) {
        return getDate(key, null);
    }

    public Date getDate(T key, Date defaultValue) {
        return get(key) != null ? (Date)get(key) : defaultValue;
    }

    public Options put(T key, Object value) {
        super.put(key, value);

        return this;
    }

    public boolean has(T key) {
        return containsKey(key);
    }

    public Options<T> merge(Options<T> otherOptions) {
        Options mergedOptions = new Options();
        for (T key : this.keySet()) {
            mergedOptions.put(key, this.get(key));
        }
        for (T key : otherOptions.keySet()) {
            mergedOptions.put(key, otherOptions.get(key));
        }
        return mergedOptions;
    }
}

