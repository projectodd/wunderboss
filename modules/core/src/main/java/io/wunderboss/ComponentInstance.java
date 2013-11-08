package io.wunderboss;

public class ComponentInstance {

    public ComponentInstance(Component component, Options options) {
        this.component = component;
        this.options = options;
    }

    public void stop() {
        if (!isStopped()) {
            component.stop(this);
            stopped = true;
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public Options getOptions() {
        return options;
    }

    private Component component;
    private Options options;
    private boolean stopped;
}
