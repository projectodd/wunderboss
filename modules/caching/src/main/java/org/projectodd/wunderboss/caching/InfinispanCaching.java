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

import org.infinispan.manager.CacheContainer;
import org.projectodd.wunderboss.Options;

public class InfinispanCaching implements Caching {

    public InfinispanCaching(String name, Options<CreateOption> options) {
        this.name = name;
    }

    @Override
    public synchronized void start() throws Exception {
        if (!started) {
            this.started = true;
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        if (started) {
            this.started = false;
        }
    }

    @Override
    public String name() {
        return this.name;
    }

    public CacheContainer cacheContainer() {
        if (this.started) {
            return this.cacheContainer;
        }
        return null;
    }

    private final String name;
    private boolean started = false;
    private CacheContainer cacheContainer;
}
