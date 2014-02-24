package org.projectodd.wunderboss.web;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.Headers;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Application;
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.ComponentInstance;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;

import java.io.File;
import java.io.IOException;

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
                .setHandler(Handlers.date(Handlers.header(pathHandler, Headers.SERVER_STRING, "undertow")))
                .build();
    }

    @Override
    public ComponentInstance start(Application application, Options options) {
        String context = options.getString("context");
        HttpHandler httpHandler = application.coerceObjectToClass(options.get("http_handler"), HttpHandler.class);

        if (options.containsKey("static_dir")) {
            final ResourceManager resourceManager = new CachingResourceManager(1000, 1L, null,
                    new FileResourceManager(new File(options.getString("static_dir")), 1 * 1024 * 1024), 250);
            final ResourceHandler resourceHandler = new ResourceHandler()
                    .setResourceManager(resourceManager)
                    .setDirectoryListingEnabled(false);
            httpHandler = new PredicateHandler(new Predicate() {
                @Override
                public boolean resolve(HttpServerExchange value) {
                    try {
                        if (value.getRelativePath().length() > 0 && !value.getRelativePath().equals("/")) {
                            return resourceManager.getResource(value.getRelativePath()) != null;
                        }
                        return false;
                    } catch (IOException ex) {
                        return false;
                    }
                }
            }, resourceHandler, httpHandler);
        }

        registerHttpHandler(context, httpHandler);

        Options instanceOptions = new Options();
        instanceOptions.put("context", context);
        ComponentInstance instance = new ComponentInstance(this, instanceOptions);
        return instance;
    }

    @Override
    public void stop(ComponentInstance instance) {
        String context = instance.getOptions().getString("context");
        unregisterHttpHandler(context);
    }

    protected void registerHttpHandler(String context, HttpHandler httpHandler) {
        pathHandler.addPrefixPath(context, httpHandler);
        if (!started) {
            undertow.start();
            log.info("Undertow listening on " + host + ":" + port);
            started = true;
        }
        log.info("Started web context " + context);
    }

    protected void unregisterHttpHandler(String context) {
        pathHandler.removePrefixPath(context);
        log.info("Stopped web context " + context);
    }

    private int port;
    private String host;
    private Undertow undertow;
    private PathHandler pathHandler = new PathHandler();
    private boolean started;

    private static final Logger log = Logger.getLogger(WebComponent.class);
}
