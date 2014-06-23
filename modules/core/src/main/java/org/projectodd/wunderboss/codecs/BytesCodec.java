package org.projectodd.wunderboss.codecs;

public abstract class BytesCodec <D> extends BaseCodec<byte[],D> {

    public BytesCodec(String name, String contentType) {
        super(name, contentType);
    }

    @Override
    public Class<byte[]> encodesTo() {
        return byte[].class;
    }
}
