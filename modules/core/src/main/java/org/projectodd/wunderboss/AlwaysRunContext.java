package org.projectodd.wunderboss;

public class AlwaysRunContext implements SingletonContext {

    public AlwaysRunContext(String name) {
        this.name = name;
    }

    @Override
    public SingletonContext runnable(Runnable r) {
        this.runnable = r;

        return this;
    }

    @Override
    public void run() {
        this.runnable.run();
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public Void implementation() {
        return null;
    }

    @Override
    public String name() {
        return this.name;
    }

    protected Runnable runnable() {
        return runnable;
    }

    private final String name;
    private Runnable runnable;

}
