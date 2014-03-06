package org.projectodd.wunderboss;

public abstract class Component<T> {

    protected abstract void configure(Options opts);

    public abstract void start();

    public abstract void stop();

    //TODO: name betterer
    public abstract T backingObject();

    protected void setName(String name) {
        this.name = name;
    }

    public String name() { return name; }

    private String name;

}
