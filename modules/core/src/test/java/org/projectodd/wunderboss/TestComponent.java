package org.projectodd.wunderboss;

public class TestComponent extends Component {

    @Override
    public void boot() {
        registered = true;
    }

    @Override
    public void shutdown() {
        stopped = true;
    }

    @Override
    public void configure(Options options) {
        configOptions = options;
    }

    @Override
    public ComponentInstance start(Application application, Options options) {
        started = true;
        startOptions = options;
        return new ComponentInstance(this, new Options());
    }

    @Override
    public void stop(ComponentInstance instance) {

    }

    boolean registered;
    Options configOptions;
    Options startOptions;
    boolean started;
    boolean stopped;
}
