package org.projectodd.wunderboss.codecs;

public abstract class BaseCodec<E,D> implements Codec<E,D>{
    public BaseCodec(String name, String contentType) {
        this.name = name;
        this.contentType = contentType;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String contentType() {
        return this.contentType;
    }


    private final String name;
    private final String contentType;
}
