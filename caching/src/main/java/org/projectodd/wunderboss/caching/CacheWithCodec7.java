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
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.projectodd.wunderboss.codecs.Codec;
import java.util.AbstractCollection;
import java.util.AbstractMap.SimpleImmutableEntry;


public class CacheWithCodec7 extends CacheWithCodec {

    public CacheWithCodec7(Cache cache, Codec codec) {
        super(cache, codec);
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
        public void remove() {
            throw new UnsupportedOperationException();
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
            return CacheWithCodec7.this.size();
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
