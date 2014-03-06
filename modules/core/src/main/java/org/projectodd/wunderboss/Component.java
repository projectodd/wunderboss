package org.projectodd.wunderboss;

public interface Component<T> {

    void start();

    void stop();

    T implementation();

    String name();
}
