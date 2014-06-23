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
