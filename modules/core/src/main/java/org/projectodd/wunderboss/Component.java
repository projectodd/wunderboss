package org.projectodd.wunderboss;

public interface Component<T> {

    void start() throws Exception;

    void stop() throws Exception;

    T implementation();

    String name();
}
