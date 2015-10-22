/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.caching;

import org.infinispan.Cache;
import org.infinispan.AbstractDelegatingCache;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.io.Serializable;


public class KeyEquivalenceCache extends AbstractDelegatingCache {

    public KeyEquivalenceCache(Cache cache) {
        super(cache);
    }

    Object encode(Object key) {
        if (key==null) {
            return null;
        } else if (byte[].class == key.getClass()) {
            return new KeyWrapper(key);
        } else {
            return key;
        }
    }

    Object decode(Object key) {
        if (key==null) {
            return null;
        } else if (key instanceof KeyWrapper) {
            return ((KeyWrapper) key).getKey();
        } else {
            return key;
        }
    }

    @Override
    public void putForExternalRead(Object key, Object value) {
        super.putForExternalRead(encode(key), value);
    }

    @Override
    public void evict(Object key) {
        super.evict(encode(key));
    }

    @Override
    public Object put(Object key, Object value, long lifespan, TimeUnit unit) {
        return super.put(encode(key), value, lifespan, unit);
    }

    @Override
    public Object putIfAbsent(Object key, Object value, long lifespan, TimeUnit unit) {
        return super.putIfAbsent(encode(key), value, lifespan, unit);
    }

    @Override
    public void putAll(Map map, long lifespan, TimeUnit unit) {
        for (Object o: map.entrySet()) {
            Entry entry = (Entry) o;
            super.put(encode(entry.getKey()), entry.getValue(), lifespan, unit);
        }
    }

    @Override
    public Object replace(Object key, Object value, long lifespan, TimeUnit unit) {
        return super.replace(encode(key), value, lifespan, unit);
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object value, long lifespan, TimeUnit unit) {
        return super.replace(encode(key), oldValue, value, lifespan, unit);
    }

    @Override
    public Object put(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return super.put(encode(key), value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public Object putIfAbsent(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return super.putIfAbsent(encode(key), value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public void putAll(Map map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        for (Object o: map.entrySet()) {
            Entry entry = (Entry) o;
            super.put(encode(entry.getKey()), entry.getValue(), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        }
    }

    @Override
    public Object replace(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return super.replace(encode(key), value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return super.replace(encode(key), oldValue, value, lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        return super.putIfAbsent(encode(key), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return super.remove(encode(key), value);
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        return super.replace(encode(key), oldValue, newValue);
    }

    @Override
    public Object replace(Object key, Object value) {
        return super.replace(encode(key), value);
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(encode(key));
    }

    @Override
    public Object get(Object key) {
        return super.get(encode(key));
    }

    @Override
    public Object put(Object key, Object value) {
        return super.put(encode(key), value);
    }

    @Override
    public Object remove(Object key) {
        return super.remove(encode(key));
    }

    @Override
    public void putAll(Map t) {
        for (Object o: t.entrySet()) {
            Entry entry = (Entry) o;
            super.put(encode(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public Set keySet() {
        Set keys = super.keySet();
        Set result = new HashSet(keys.size());
        for (Object k: keys) {
            result.add(decode(k));
        }
        return result;
    }

    @Override
    public Set entrySet() {
        Set entries = super.entrySet();
        Set result = new HashSet(entries.size());
        for (Object o: entries) {
            Entry entry = (Entry) o;
            result.add(new SimpleImmutableEntry(decode(entry.getKey()), entry.getValue()));
        }
        return result;
    }

    static class KeyWrapper implements Serializable {
        public KeyWrapper(Object key) {
            this.key = key;
        }
        public Object getKey() {
            return this.key;
        }
        public boolean equals(Object obj) {
            return obj instanceof KeyWrapper ? 
                obj.equals(key) :
                equiv.equals(obj, key);
        }
        public int hashCode() {
            return equiv.hashCode(key);
        }
        public String toString() {
            return equiv.toString(key);
        }
        private final Object key;
        static final Equivalence equiv = new Equivalence();
    }
}
