package org.projectodd.wunderboss;

/**
 * A non-io specific closeable.
 */
public interface Closeable {
    void close() throws Exception;
}
