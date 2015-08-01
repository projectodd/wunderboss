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
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.projectodd.wunderboss.codecs.Codec;
import java.util.concurrent.TimeUnit;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;


public class CacheWithCodec extends AbstractDelegatingCache {

    public CacheWithCodec(Cache cache, Codec codec) {
        super(cache);
        this.codec = codec;
    }

    Object encode(Object value) {
        return codec.encode(value);
    }

    Object decode(Object value) {
        return codec.decode(value);
    }

    @Override
    public void putForExternalRead(Object key, Object value) {
        super.putForExternalRead(encode(key), encode(value));
    }

    @Override
    public void evict(Object key) {
        super.evict(encode(key));
    }

    @Override
    public Object put(Object key, Object value, long lifespan, TimeUnit unit) {
        return decode(super.put(encode(key), encode(value), lifespan, unit));
    }

    @Override
    public Object putIfAbsent(Object key, Object value, long lifespan, TimeUnit unit) {
        return decode(super.putIfAbsent(encode(key), encode(value), lifespan, unit));
    }

    @Override
    public void putAll(Map map, long lifespan, TimeUnit unit) {
        for (Object o: map.entrySet()) {
            Entry entry = (Entry) o;
            super.put(encode(entry.getKey()), encode(entry.getValue()), lifespan, unit);
        }
    }

    @Override
    public Object replace(Object key, Object value, long lifespan, TimeUnit unit) {
        return decode(super.replace(encode(key), encode(value), lifespan, unit));
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object value, long lifespan, TimeUnit unit) {
        return super.replace(encode(key), encode(oldValue), encode(value), lifespan, unit);
    }

    @Override
    public Object put(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return decode(super.put(encode(key), encode(value), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit));
    }

    @Override
    public Object putIfAbsent(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return decode(super.putIfAbsent(encode(key), encode(value), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit));
    }

    @Override
    public void putAll(Map map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        for (Object o: map.entrySet()) {
            Entry entry = (Entry) o;
            super.put(encode(entry.getKey()), encode(entry.getValue()), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
        }
    }

    @Override
    public Object replace(Object key, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return decode(super.replace(encode(key), encode(value), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit));
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
        return super.replace(encode(key), encode(oldValue), encode(value), lifespan, lifespanUnit, maxIdleTime, maxIdleTimeUnit);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        return decode(super.putIfAbsent(encode(key), encode(value)));
    }

    @Override
    public boolean remove(Object key, Object value) {
        return super.remove(encode(key), encode(value));
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        return super.replace(encode(key), encode(oldValue), encode(newValue));
    }

    @Override
    public Object replace(Object key, Object value) {
        return decode(super.replace(encode(key), encode(value)));
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(encode(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(encode(value));
    }

    @Override
    public Object get(Object key) {
        return decode(super.get(encode(key)));
    }

    @Override
    public Object put(Object key, Object value) {
        return decode(super.put(encode(key), encode(value)));
    }

    @Override
    public Object remove(Object key) {
        return decode(super.remove(encode(key)));
    }

    @Override
    public void putAll(Map t) {
        for (Object o: t.entrySet()) {
            Entry entry = (Entry) o;
            super.put(encode(entry.getKey()), encode(entry.getValue()));
        }
    }

    @Override
    public CloseableIteratorSet keySet() {
        return new EncodedSet(super.keySet());
    }

    @Override
    public CloseableIteratorSet entrySet() {
        return new EncodedEntrySet(super.entrySet());
    }

    @Override
    public CloseableIteratorCollection values() {
        return new EncodedCollection(super.values());
    }

    private Codec codec;

    class Decoded implements CloseableIterator {
        Decoded(CloseableIterator iterator) {
            this.iterator = iterator;
        }
        public void close() {
            iterator.close();
        }
        public boolean hasNext() {
            return iterator.hasNext();
        }
        public Object next() {
            return decode(iterator.next());
        }
        protected CloseableIterator iterator;
    }

    class DecodedEntry extends Decoded {
        DecodedEntry(CloseableIterator iterator) {
            super(iterator);
        }
        public Object next() {
            Entry e = (Entry) iterator.next();
            return new SimpleImmutableEntry(decode(e.getKey()), decode(e.getValue()));
        }
    }

    class EncodedCollection extends AbstractCollection implements CloseableIteratorCollection {
        EncodedCollection(CloseableIteratorCollection collection) {
            this.collection = collection;
        }
        public int size() {
            return CacheWithCodec.this.size();
        }
        public CloseableIterator iterator() {
            return new Decoded(collection.iterator());
        }
        protected CloseableIteratorCollection collection;
    }

    class EncodedSet extends EncodedCollection implements CloseableIteratorSet {
        EncodedSet(CloseableIteratorCollection collection) {
            super(collection);
        }
    }

    class EncodedEntrySet extends EncodedSet {
        EncodedEntrySet(CloseableIteratorCollection collection) {
            super(collection);
        }
        public CloseableIterator iterator() {
            return new DecodedEntry(collection.iterator());
        }
    }
}
