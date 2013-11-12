package io.wunderboss.web;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.wunderboss.Application;
import io.wunderboss.Component;
import io.wunderboss.ComponentInstance;
import io.wunderboss.Options;
import org.jboss.logging.Logger;

public class WebComponent extends Component {
    @Override
    public void boot() {
        configure(new Options());
        // TODO: Configurable non-lazy boot of Undertow
    }

    @Override
    public void shutdown() {
        if (started) {
            undertow.stop();
            log.info("Undertow stopped");
            started = false;
        }
    }

    @Override
    public void configure(Options options) {
        port = options.getInt("port", 8080);
        host = options.getString("host", "localhost");
        undertow = Undertow.builder()
                .addListener(port, host)
                .setHandler(pathHandler)
                .build();
    }

    @Override
    public ComponentInstance start(Application application, Options options) {
        String context = options.getString("context");
        HttpHandler httpHandler = application.coerceObjectToClass(options.get("http_handler"), HttpHandler.class);

        pathHandler.addPath(context, httpHandler);

        if (!started) {
            undertow.start();
            log.info("Undertow listening on " + host + ":" + port);
            started = true;
        }
        log.info("Started web context " + context);

        Options instanceOptions = new Options();
        instanceOptions.put("context", context);
        ComponentInstance instance = new ComponentInstance(this, instanceOptions);
        return instance;
    }

    @Override
    public void stop(ComponentInstance instance) {
        String context = instance.getOptions().getString("context");
        pathHandler.removePath(context);
        log.info("Stopped web context " + context);
    }

    private int port;
    private String host;
    private Undertow undertow;
    private PathHandler pathHandler = new PathHandler();
    private boolean started;

    private static final Logger log = Logger.getLogger(WebComponent.class);
}
