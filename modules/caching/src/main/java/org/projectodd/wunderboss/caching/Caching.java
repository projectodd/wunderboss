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
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Option;
import org.projectodd.wunderboss.codecs.Codec;

import java.util.Map;

public interface Caching extends Component {
    class CreateOption extends Option {
        public static final CreateOption CONFIGURATION = opt("configuration",               CreateOption.class);
        public static final CreateOption TRANSACTIONAL = opt("transactional", false,        CreateOption.class);
        public static final CreateOption LOCKING       = opt("locking",       "OPTIMISTIC", CreateOption.class);
        public static final CreateOption PERSIST       = opt("persist",       false,        CreateOption.class);
        public static final CreateOption MODE          = opt("mode",                        CreateOption.class);
        public static final CreateOption EVICTION      = opt("eviction",      "NONE",       CreateOption.class);
        public static final CreateOption MAX_ENTRIES   = opt("max_entries",   -1,           CreateOption.class);
        public static final CreateOption IDLE          = opt("idle",          -1,           CreateOption.class);
        public static final CreateOption TTL           = opt("ttl",           -1,           CreateOption.class);
    }
    Cache findOrCreate(String name, Map<CreateOption,Object> options);
    Cache find(String name);
    Cache withCodec(Cache cache, Codec codec);
    Cache withExpiration(Cache cache, long ttl, long idle);
    boolean stop(String name);
}
