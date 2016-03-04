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

package org.projectodd.wunderboss.caching;

import org.infinispan.Cache;
import org.infinispan.CacheSet;
import org.infinispan.CacheCollection;
import org.infinispan.CacheStream;
import org.infinispan.commons.util.Closeables;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.commons.util.CloseableSpliterator;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.stream.impl.local.ValueCacheCollection;
import org.infinispan.stream.impl.local.EntryStreamSupplier;
import org.infinispan.stream.impl.local.LocalCacheStream;
import org.projectodd.wunderboss.codecs.Codec;
import java.util.AbstractCollection;
import java.util.function.Function;


public class CacheWithCodec8 extends CacheWithCodec {

    public CacheWithCodec8(Cache cache, Codec codec) {
        super(cache, codec);
    }

    @Override
    public CacheSet keySet() {
        return new EncodedCache(super.keySet(), x -> decode(x));
    }

    @Override
    public CacheSet entrySet() {
        return new EncodedCache(super.entrySet(), 
                                x -> {
                                    Entry e = (Entry) x;
                                    return new ImmortalCacheEntry(decode(e.getKey()),
                                                                  decode(e.getValue()));
                                });
    }

    @Override
    public CacheCollection values() {
        return new ValueCacheCollection(this, entrySet());
    }

    class EncodedCache extends AbstractCollection implements CacheSet {

        EncodedCache(CacheSet delegate, Function mapper) {
            this.delegate = delegate;
            this.mapper = mapper;
        }

        @Override
        public int size() {
            return CacheWithCodec8.this.size();
        }

        @Override
        public CacheStream stream() {
            return new LocalCacheStream(new EntryStreamSupplier(cache, getConsistentHash(cache),
                                                                () -> delegate.stream().map(mapper)),
                                        false,
                                        cache.getAdvancedCache().getComponentRegistry());

        }

        @Override
        public CacheStream parallelStream() {
            return new LocalCacheStream(new EntryStreamSupplier(cache, getConsistentHash(cache),
                                                                () -> delegate.stream().map(mapper)),
                                        true,
                                        cache.getAdvancedCache().getComponentRegistry());
        }

        @Override
        public CloseableIterator iterator() {
            return Closeables.iterator(stream());
        }

        @Override
        public CloseableSpliterator spliterator() {
            return Closeables.spliterator(stream());
        }

        private ConsistentHash getConsistentHash(Cache cache) {
            DistributionManager dm = cache.getAdvancedCache().getDistributionManager();
            return (dm != null) ? dm.getReadConsistentHash() : null;
        }

        private CacheSet delegate;
        private Function mapper;
        private Cache cache = CacheWithCodec8.this;
    }
}
