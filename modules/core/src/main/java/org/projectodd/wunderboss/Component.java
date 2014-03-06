package org.projectodd.wunderboss;

public interface Component<T> {

    public abstract void start();

    public abstract void stop();

    public abstract T implementation();

    public String name();
}
