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
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;

import org.projectodd.wunderboss.Options;

public class InfinispanCaching implements Caching {

    public InfinispanCaching(String name, Options<CreateOption> options) {
        this.manager = new DefaultCacheManager((Configuration) options.get(CreateOption.CONFIGURATION), false);
        this.name = name;
    }

    @Override
    public synchronized void start() throws Exception {
        if (!started) {
            this.manager.start();
            this.started = true;
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        if (started) {
            this.manager.stop();
            this.started = false;
        }
    }

    @Override
    public String name() {
        // manager.getName() ?
        return this.name;
    }

    @Override
    public Cache find(String name) {
        Cache result = null;
        if (manager.isRunning(name)) {
            result = manager.getCache(name);
            if (!result.getStatus().allowInvocations()) {
                result.start();
            }
        }
        return result;
    }
    
    @Override
    public Cache create(String name, Configuration configuration) {
        if (null != find(name)) {
            log.warn("Removing existing cache: "+name);
            manager.removeCache(name);
        }
        manager.defineConfiguration(name, configuration);
        log.info("Creating cache: "+name);
        return manager.getCache(name);
    }

    @Override
    public Cache findOrCreate(String name, Configuration configuration) {
        Cache result = find(name);
        return (result == null) ? create(name, configuration) : result;
    }

    public EmbeddedCacheManager manager() {
        if (this.started) {
            return this.manager;
        }
        return null;
    }

    private final String name;
    private boolean started = false;
    private EmbeddedCacheManager manager;

    private static final Logger log = Logger.getLogger(Caching.class);
}
