package org.projectodd.wunderboss.codecs;

public abstract class StringCodec<D> extends BaseCodec<String,D> {

    public StringCodec(String name, String contentType) {
        super(name, contentType);
    }

    @Override
    public Class<String> encodesTo() {
        return String.class;
    }
}
