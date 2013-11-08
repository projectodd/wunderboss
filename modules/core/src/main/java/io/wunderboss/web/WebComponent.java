package io.wunderboss.web;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.wunderboss.Component;
import io.wunderboss.ComponentInstance;
import io.wunderboss.Options;
import org.jboss.logging.Logger;

public class WebComponent extends Component {
    @Override
    public void boot() {
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
        port = Integer.parseInt((String) options.get("port", "8080"));
        host = (String) options.get("host", "localhost");
        undertow = Undertow.builder()
                .addListener(port, host)
                .setHandler(pathHandler)
                .build();
    }

    @Override
    public ComponentInstance start(Options options) {
        String context = (String) options.get("context");
        HttpHandler httpHandler = (HttpHandler) options.get("http_handler");

        pathHandler.addPath(context, httpHandler);

        if (!started) {
            undertow.start();
            log.info("Undertow listening on " + host + ":" + port);
            started = true;
        }

        Options instanceOptions = new Options();
        instanceOptions.put("context", context);
        ComponentInstance instance = new ComponentInstance(this, instanceOptions);
        return instance;
    }

    @Override
    public void stop(ComponentInstance instance) {
        String context = (String) instance.getOptions().get("context");
        pathHandler.removePath(context);
    }

    private int port;
    private String host;
    private Undertow undertow;
    private PathHandler pathHandler = new PathHandler();
    private boolean started;

    private static final Logger log = Logger.getLogger(WebComponent.class);
}
