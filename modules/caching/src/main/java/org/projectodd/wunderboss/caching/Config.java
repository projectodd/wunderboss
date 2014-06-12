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

public class Config {

    public static Configuration uration(Options<Caching.CreateOption> options) {
        return builder(options).build();
    }

    public static ConfigurationBuilder builder(Options<Caching.CreateOption> options) {
        ConfigurationBuilder result = new ConfigurationBuilder();
        read(result, options);
        mode(result, options);
        eviction(result, options);
        expiration(result, options);
        transactional(result, options);
        return result;
    }

    public static void read(ConfigurationBuilder builder, Options<Caching.CreateOption> options) {
        Configuration c = (Configuration) options.get(Caching.CreateOption.CONFIGURATION);
        if (c != null) {
            builder.read(c);
        }
    }
    public static void mode(ConfigurationBuilder builder, Options<Caching.CreateOption> options) {
        builder.clustering().cacheMode(CacheMode.valueOf(options.getString(Caching.CreateOption.MODE).toUpperCase()));
    }
    public static void eviction(ConfigurationBuilder builder, Options<Caching.CreateOption> options) {
        builder.eviction()
            .strategy(EvictionStrategy.valueOf(options.getString(Caching.CreateOption.EVICTION).toUpperCase()))
            .maxEntries(options.getInt(Caching.CreateOption.MAX_ENTRIES));
    }
    public static void expiration(ConfigurationBuilder builder, Options<Caching.CreateOption> options) {
        builder.expiration()
            .maxIdle(options.getLong(Caching.CreateOption.IDLE))
            .lifespan(options.getLong(Caching.CreateOption.TTL));
    }
    public static void transactional(ConfigurationBuilder builder, Options<Caching.CreateOption> options) {
        if (options.getBoolean(Caching.CreateOption.TRANSACTIONAL)) {
            LockingMode mode = LockingMode.valueOf(options.getString(Caching.CreateOption.LOCKING).toUpperCase());
            builder.transaction()
                .transactionMode(TransactionMode.TRANSACTIONAL)
                .transactionManagerLookup(new GenericTransactionManagerLookup())
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

}
