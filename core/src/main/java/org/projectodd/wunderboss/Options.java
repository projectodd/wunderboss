/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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
import java.util.List;
import java.util.Arrays;

public class Options<T> extends HashMap<T, Object> {

    public Options() {
        this(null);
    }

    public Options(Map<T, Object> options) {
        if (options != null) {
            putAll(options);
        }
    }

    /**
     * Looks up key in the options.
     *
     * If key is an Option, its default value will be returned if the key
     * isn't found.
     *
     * @param key
     * @return
     */
    public Object get(Object key) {
        Object val = super.get(key);
        if (val == null && key instanceof Option) {
            val = ((Option)key).defaultValue;
        }

        return val;
    }

    public Object get(T key, Object defaultValue) {
        return get(key) != null ? get(key) : defaultValue;
    }

    public Boolean getBoolean(T key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(T key, Boolean defaultValue) {
        Object value = get(key);
        if (value == null) {
            value = defaultValue;
        } else if (!(value instanceof Boolean)) {
            value = Boolean.parseBoolean(value.toString());
        }

        return (Boolean)value;
    }

    public Integer getInt(T key) {
        return getInt(key, null);
    }

    public Integer getInt(T key, Integer defaultValue) {
        Object value = get(key);
        if (value == null) {
            value = defaultValue;
        } else if (!(value instanceof Integer)) {
            value = Integer.parseInt(value.toString());
        }

        return (Integer)value;
    }

    public Long getLong(T key) {
        return getLong(key, null);
    }

    public Long getLong(T key, Long defaultValue) {
        Object value = get(key);
        if (value == null) {
            value = defaultValue;
        } else if (!(value instanceof Long)) {
            value = Long.parseLong(value.toString());
        }

        return (Long)value;
    }

    public Double getDouble(T key) {
        return getDouble(key, null);
    }

    public Double getDouble(T key, Double defaultValue) {
        Object value = get(key);
        if (value == null) {
            value = defaultValue;
        } else if (!(value instanceof Double)) {
            value = Double.parseDouble(value.toString());
        }

        return (Double)value;
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

    public List getList(T key) {
        Object v = get(key);
        if (v instanceof List || v == null) {
            return (List) v;
        }
        return Arrays.asList(v);
    }

    public Options put(T key, Object value) {
        super.put(key, value);

        return this;
    }

    /**
     * Returns true if key resolves to a *non-null value* (considering
     * a possible default), false otherwise.
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
