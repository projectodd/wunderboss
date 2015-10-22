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
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.codecs.Codec;

import java.util.Map;

public class InfinispanCaching implements Caching {

    public InfinispanCaching(String name, Options<CreateOption> options) {
        this.name = name;
        this.options = options;
    }

    @Override
    public synchronized void start() throws Exception {
        manager().start();
    }

    @Override
    public synchronized void stop() throws Exception {
        manager().stop();
    }

    @Override
    public boolean isRunning() {
        return manager().getStatus().allowInvocations();
    }

    @Override
    public String name() {
        // manager().getName() ?
        return this.name;
    }

    @Override
    public Cache find(String name) {
        Cache result = null;
        if (manager().isRunning(name)) {
            result = manager().getCache(name);
            if (!result.getStatus().allowInvocations()) {
                result.start();
            }
        }
        return result;
    }
    
    @Override
    public Cache findOrCreate(String name, Map<CreateOption,Object> options) {
        Cache result = find(name);
        if (result == null) {
            manager().defineConfiguration(name, Config.uration(validate(options)));
            log.info("Creating cache: "+name);
            result = manager().getCache(name);
        }
        return result;
    }

    @Override
    public boolean stop(String name) {
        EmbeddedCacheManager mgr = manager();
        boolean before = mgr.isRunning(name);
        mgr.removeCache(name);
        return before != mgr.isRunning(name);
    }

    @Override
    public Cache withCodec(Cache cache, Codec codec) {
        return (codec==null) ? cache : encoder.encode(cache, codec);
    }

    @Override
    public Cache withExpiration(Cache cache, long ttl, long idle) {
        return new CacheWithExpiration(cache, ttl, idle);
    }

    public synchronized EmbeddedCacheManager manager() {
        if (this.manager == null) {
            this.manager = new DefaultCacheManager(Config.uration(options));
        }
        return this.manager;
    }

    protected Options<CreateOption> validate(Map<CreateOption,Object> options) {
        Options<CreateOption> result = new Options<CreateOption>(options);
        String mode = result.getString(CreateOption.MODE);
        if (mode != null && !"LOCAL".equalsIgnoreCase(mode)) {
            log.warn("Requested mode only available in a cluster, setting to LOCAL");
            result.put(CreateOption.MODE, "LOCAL");
        }
        return result;
    }

    private final String name;
    protected final Options options;
    protected EmbeddedCacheManager manager;
    protected Encoder encoder = new Encoder7();

    protected static final Logger log = Logger.getLogger(Caching.class);
}
