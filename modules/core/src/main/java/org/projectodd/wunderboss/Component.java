package org.projectodd.wunderboss;

public abstract class Component {

    public String[] getLanguageDependencies() {
        return new String[]{};
    }

    public String[] getComponentDependencies() {
        return new String[]{};
    }

    public void setContainer(WunderBoss container) {
        this.container = container;
    }

    public abstract void boot();

    public abstract void shutdown();

    public abstract void configure(Options options);

    public abstract ComponentInstance start(Application application, Options options);

    public abstract void stop(ComponentInstance instance);

    protected WunderBoss getContainer() {
        return container;
    }

    private WunderBoss container;
    private Application application;
}
