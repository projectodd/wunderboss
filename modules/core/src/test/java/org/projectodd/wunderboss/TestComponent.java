package org.projectodd.wunderboss;

public class TestComponent extends Component {

    @Override
    public void start() {
        registered = true;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public Object backingObject() {
        return null;
    }

    @Override
    protected void configure(Options options) {
        configOptions = options;
    }

    boolean registered;
    Options configOptions;
    Options startOptions;
    boolean started;
    boolean stopped;
}
