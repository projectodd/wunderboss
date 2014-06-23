package org.projectodd.wunderboss.codecs;

public abstract class ObjectCodec<D> extends BaseCodec<Object,D> {

    public ObjectCodec(String name, String contentType) {
        super(name, contentType);
    }

    @Override
    public Class<Object> encodesTo() {
        return Object.class;
    }
}
