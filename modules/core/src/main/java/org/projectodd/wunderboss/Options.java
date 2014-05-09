/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    public Boolean getBoolean(T key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(T key, Boolean defaultValue) {
        Object value = get(key);
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
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

    public Long getLong(T key) {
        return getLong(key, null);
    }

    public Long getLong(T key, Long defaultValue) {
        Object value = get(key);
        if (value != null) {
            return Long.parseLong(value.toString());
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

    public String removeString(T key, String defaultValue) {
        return get(key) != null ? remove(key).toString() : defaultValue;
    }

    public Options put(T key, Object value) {
        super.put(key, value);

        return this;
    }

    /**
     * Returns true if key has a *non-null value* in the map, false otherwise.
     */
    public boolean has(T key) {
        return get(key) != null;
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

