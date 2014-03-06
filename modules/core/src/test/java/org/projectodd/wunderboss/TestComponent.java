package org.projectodd.wunderboss;

public class TestComponent implements Component {

    public TestComponent(String name, Options opts) {
        this.name = name;
        this.configOptions = opts;
    }

    @Override
    public void start() {
        registered = true;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public Object implementation() {
        return null;
    }

    @Override
    public String name() {
        return this.name;
    }

    String name;
    boolean registered;
    Options configOptions;
    boolean stopped;
}
