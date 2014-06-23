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

package org.projectodd.wunderboss.codecs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Codecs {

    public Codecs add(Codec codec) {
        codecs.put(codec.name(), codec);
        codecs.put(codec.contentType(), codec);

        return this;
    }

    public Codec forName(String name) {
        return codecs.get(name);
    }

    public Codec forContentType(String contentType) {
        return codecs.get(contentType);
    }

    public Collection<Codec> codecs() {
        return Collections.unmodifiableCollection(codecs.values());
    }

    private final Map<String, Codec> codecs = new HashMap<>();
}
