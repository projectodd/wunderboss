package io.wunderboss;

public class ComponentInstance {

    public ComponentInstance(Component component, Options options) {
        this.component = component;
        this.options = options;
    }

    public void stop() {
        component.stop(this);
    }

    public Options getOptions() {
        return options;
    }

    private Component component;
    private Options options;
}
