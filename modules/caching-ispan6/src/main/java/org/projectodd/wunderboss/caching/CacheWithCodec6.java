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
import org.projectodd.wunderboss.codecs.Codec;
import java.util.Collection;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;


public class CacheWithCodec6 extends CacheWithCodec {

    public CacheWithCodec6(Cache cache, Codec codec) {
        super(cache, codec);
    }

    @Override
    public Set keySet() {
        Set keys = super.keySet();
        Set result = new HashSet(keys.size());
        for (Object k: keys) {
            result.add(decode(k));
        }
        return result;
    }

    @Override
    public Set entrySet() {
        Set entries = super.entrySet();
        Set result = new HashSet(entries.size());
        for (Object o: entries) {
            Entry entry = (Entry) o;
            result.add(new SimpleImmutableEntry(decode(entry.getKey()), decode(entry.getValue())));
        }
        return result;
    }

    @Override
    public Collection values() {
        Collection values = super.values();
        Collection result = new ArrayList(values.size());
        for (Object v: values) {
            result.add(decode(v));
        }
        return result;
    }
}
