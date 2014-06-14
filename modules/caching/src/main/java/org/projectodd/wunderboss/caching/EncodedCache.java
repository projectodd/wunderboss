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
package org.projectodd.wunderboss.caching;

import org.infinispan.Cache;
import org.infinispan.AbstractDelegatingCache;
import org.infinispan.util.concurrent.NotifyingFuture;
import org.projectodd.wunderboss.codecs.Codec;
import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;


public class EncodedCache<K,V> extends AbstractDelegatingCache<K,V> {

    public EncodedCache(Cache<K,V> cache, Codec codec) {
        super(cache);
        this.codec = codec;
    }

    K encodeKey(Object key) {
        return (K) codec.encode(key);
    }

    V encode(Object value) {
        return (V) codec.encode(value);
    }

    K decodeKey(Object key) {
        return (K) codec.decode(key);
    }

    V decode(Object value) {
        return (V) codec.decode(value);
    }

    @Override
    public void putForExternalRead(K key, V value) {
        super.putForExternalRead(encodeKey(key), encode(value));
    }

    @Override
    public void evict(K key) {
        super.evict(encodeKey(key));
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit unit) {
        return decode(super.put(encodeKey(key), encode(value), lifespan, unit));
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit unit) {
        return decode(super.putIfAbsent(encodeKey(key), encode(value), lifespan, unit));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit unit) {
        for (Entry entry: map.entrySet()) {
            super.put(encodeKey(entry.getKey()), encode(entry.getValue()), lifespan, unit);
        }
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit unit) {
        return decode(super.replace(encodeKey(key), encode(value), lifespan, unit));
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit unit) {
        return super.replace(encodeKey(key), encode(oldValue), encode(value), lifespan, unit);
    }

    @Override
    public V put(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return decode(super.put(encodeKey(key), encode(value), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit));
    }

    @Override
    public V putIfAbsent(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return decode(super.putIfAbsent(encodeKey(key), encode(value), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        for (Entry entry: map.entrySet()) {
            super.put(encodeKey(entry.getKey()), encode(entry.getValue()), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        }
    }

    @Override
    public V replace(K key, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return decode(super.replace(encodeKey(key), encode(value), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit));
    }

    @Override
    public boolean replace(K key, V oldValue, V value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return super.replace(encodeKey(key), encode(oldValue), encode(value), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return decode(super.putIfAbsent(encodeKey(key), encode(value)));
    }

    @Override
    public boolean remove(Object key, Object value) {
        return super.remove(encodeKey(key), encode(value));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return super.replace(encodeKey(key), encode(oldValue), encode(newValue));
    }

    @Override
    public V replace(K key, V value) {
        return decode(super.replace(encodeKey(key), encode(value)));
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(encodeKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(encode(value));
    }

    @Override
    public V get(Object key) {
        return super.get(encodeKey(key));
    }

    @Override
    public V put(K key, V value) {
        return decode(super.put(encodeKey(key), encode(value)));
    }

    @Override
    public V remove(Object key) {
        return super.remove(encodeKey(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> t) {
        for (Entry entry: t.entrySet()) {
            super.put(encodeKey(entry.getKey()), encode(entry.getValue()));
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = super.keySet();
        Set<K> result = new HashSet<K>(keys.size());
        for (K k: keys) {
            result.add(decodeKey(k));
        }
        return result;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K,V>> entries = super.entrySet();
        Set<Entry<K,V>> result = new HashSet<Entry<K,V>>(entries.size());
        for (Entry entry: entries) {
            result.add(new SimpleImmutableEntry(decodeKey(entry.getKey()), decode(entry.getValue())));
        }
        return result;
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = super.values();
        Collection<V> result = new ArrayList<V>(values.size());
        for (V v: values) {
            result.add(decode(v));
        }
        return result;
    }

    private Codec codec;
}
