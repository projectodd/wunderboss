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
