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
import java.util.concurrent.TimeUnit;
import java.util.Map;


public class CacheWithExpiration extends AbstractDelegatingCache {

    public CacheWithExpiration(Cache cache, long ttl, long idle) {
        super(cache);
        this.ttl = ttl;
        this.idle = idle;
    }

    @Override
    public Object put(Object key, Object value) {
        return put(key, value, ttl, TimeUnit.MILLISECONDS, idle, TimeUnit.MILLISECONDS);
    }

    @Override
    public void putAll(Map t) {
        putAll(t, ttl, TimeUnit.MILLISECONDS, idle, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        return putIfAbsent(key, value, ttl, TimeUnit.MILLISECONDS, idle, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        return replace(key, oldValue, newValue, ttl, TimeUnit.MILLISECONDS, idle, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object replace(Object key, Object value) {
        return replace(key, value, ttl, TimeUnit.MILLISECONDS, idle, TimeUnit.MILLISECONDS);
    }

    private long ttl;
    private long idle;
}
