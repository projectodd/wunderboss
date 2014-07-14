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

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.util.concurrent.IsolationLevel;

import org.projectodd.wunderboss.Options;
import java.util.Arrays;


public class Config {

    public static Configuration uration(Options<Caching.CreateOption> options) {
        Configuration c = (Configuration) options.get(Caching.CreateOption.CONFIGURATION);
        return (c != null && options.size() == 1) ?
            c : builder(options).build();
    }

    public static ConfigurationBuilder builder(Options<Caching.CreateOption> options) {
        return new Config(options).builder;
    }

    Config(Options<Caching.CreateOption> options) {
        this.options = options;
        builder.dataContainer().keyEquivalence(EQUIVALENCE);
        read();
        mode();
        evict();
        expire();
        transact();
        persist();
    }

    void read() {
        Configuration c = (Configuration) options.get(Caching.CreateOption.CONFIGURATION);
        if (c != null) {
            builder.read(c);
        }
    }
    void mode() {
        String m = options.getString(Caching.CreateOption.MODE);
        if (m != null) {
            builder.clustering().cacheMode(CacheMode.valueOf(m.toUpperCase()));
        }
    }
    void evict() {
        builder.eviction()
            .strategy(EvictionStrategy.valueOf(options.getString(Caching.CreateOption.EVICTION).toUpperCase()))
            .maxEntries(options.getInt(Caching.CreateOption.MAX_ENTRIES));
    }
    void expire() {
        builder.expiration()
            .maxIdle(options.getLong(Caching.CreateOption.IDLE))
            .lifespan(options.getLong(Caching.CreateOption.TTL));
    }
    void transact() {
        if (options.getBoolean(Caching.CreateOption.TRANSACTIONAL)) {
            LockingMode mode = LockingMode.valueOf(options.getString(Caching.CreateOption.LOCKING).toUpperCase());
            builder.transaction()
                .transactionMode(TransactionMode.TRANSACTIONAL)
                .transactionManagerLookup(TM_LOOKUP)
                .lockingMode(mode)
                .recovery()
                .versioning().enabled(mode==LockingMode.OPTIMISTIC).scheme(VersioningScheme.SIMPLE)
                .locking()
                .isolationLevel(mode==LockingMode.OPTIMISTIC ? IsolationLevel.REPEATABLE_READ : IsolationLevel.READ_COMMITTED)
                .writeSkewCheck(mode==LockingMode.OPTIMISTIC);
        } else {
            builder.transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL);
        }
    }
    void persist() {
        Object v = options.get(Caching.CreateOption.PERSIST);
        if (v instanceof Boolean && (boolean) v) {
            builder.persistence().addSingleFileStore();
        }
        if (v instanceof String) {
            builder.persistence().addSingleFileStore().location(v.toString());
        }
    }     

    private Options<Caching.CreateOption> options;
    ConfigurationBuilder builder = new ConfigurationBuilder();
    static final Equivalence EQUIVALENCE = new Equivalence();
    static final GenericTransactionManagerLookup TM_LOOKUP = new GenericTransactionManagerLookup();

    static class Equivalence implements org.infinispan.commons.equivalence.Equivalence<Object> {
        private static boolean isByteArray(Object obj) {
            return byte[].class == obj.getClass();
        }
        public int hashCode(Object obj) {
            if (isByteArray(obj)) {
                return 41 + Arrays.hashCode((byte[]) obj);
            } else {
                return obj.hashCode();
            }
        }
        public boolean equals(Object obj, Object otherObj) {
            if (obj == otherObj)
                return true;
            if (obj == null || otherObj == null)
                return false;
            if (isByteArray(obj) && isByteArray(otherObj))
                return Arrays.equals((byte[]) obj, (byte[]) otherObj);
            return obj.equals(otherObj);
        }
        public String toString(Object obj) {
            if (isByteArray(obj))
                return Arrays.toString((byte[]) obj);
            else
                return obj.toString();
        }
        public boolean isComparable(Object obj) {
            return obj instanceof Comparable;
        }
        public int compare(Object obj, Object otherObj) {
            return ((Comparable<Object>) obj).compareTo(otherObj);
        }
    }
}
