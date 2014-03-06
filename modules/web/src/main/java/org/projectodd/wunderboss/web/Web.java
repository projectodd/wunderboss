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
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.Headers;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Web implements Component<Undertow> {

    public Web(String name, Options opts) {
        this.name = name;
        configure(opts);
    }

    @Override
    public String name() { return name; }

    @Override
    public void start() {
        // TODO: Configurable non-lazy boot of Undertow
        if (!started) {
            undertow.start();
            log.info("Undertow listening on " + host + ":" + port);
            started = true;
        }
    }

    @Override
    public void stop() {
        if (started) {
            undertow.stop();
            log.info("Undertow stopped");
            started = false;
        }
    }

    @Override
    public Undertow implementation() {
        return this.undertow;
    }

    private void configure(Options options) {
        port = options.getInt("port", 8080);
        host = options.getString("host", "localhost");
        undertow = Undertow.builder()
                .addListener(port, host)
                .setHandler(Handlers.date(Handlers.header(pathHandler, Headers.SERVER_STRING, "undertow")))
                .build();
    }

    public void registerHttpHandler(final String context, HttpHandler httpHandler, 
                                    Map<String, Object> opts) {
        Options options = new Options(opts);
        if (options.containsKey("static_dir")) {
            httpHandler = wrapWithStaticHandler(httpHandler, options.getString("static_dir"));
        }
        pathHandler.addPrefixPath(context, httpHandler);
        contextRegistrar.put(context, new Runnable() { 
                public void run() { 
                    pathHandler.removePrefixPath(context);
                }});
        start();
        
        log.info("Started web context " + context);
    }

    public void registerServlet(String context, Class servletClass,
                                Map<String, Object> opts) {
        Options options = new Options(opts);
        final ServletInfo servlet = Servlets.servlet(servletClass.getSimpleName(), servletClass)
                .addMapping("/*");

        final DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(WunderBoss.class.getClassLoader())
                .setContextPath(context)
                .setDeploymentName(context)
                .addServlet(servlet);

        if (options.containsKey("context_attributes")) {
            Map<String, Object> contextAttributes = (Map)options.get("context_attributes");
            for (Map.Entry<String, Object> entry : contextAttributes.entrySet()) {
                servletBuilder.addServletContextAttribute(entry.getKey(), entry.getValue());
            }
        }

        final DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        try {
            Options webOptions = new Options();
            if (options.containsKey("static_dir")) {
                webOptions.put("static_dir", options.getString("static_dir"));
            }
            registerHttpHandler(context, manager.start(), webOptions);
            contextRegistrar.put(context, new Runnable() { 
                public void run() { 
                    try {
                        manager.stop();
                        manager.undeploy();
                        Servlets.defaultContainer().removeDeployment(servletBuilder);
                    } catch (ServletException e) {
                        e.printStackTrace();
                    }}});
        } catch (ServletException e) {
            // TODO: something better
            e.printStackTrace();
        }
    }

    public boolean unregister(String context) {
        Runnable f = contextRegistrar.remove(context);
        if (f != null) {
            f.run();
            log.info("Stopped web context at path " + context);
            return true;
        } else {
            log.warn("No context registered at path " + context);
            return false;
        }
    }

    protected HttpHandler wrapWithStaticHandler(HttpHandler baseHandler, String path) {
        final ResourceManager resourceManager =
                new CachingResourceManager(1000, 1L, null,
                                           new FileResourceManager(new File(path), 1 * 1024 * 1024), 250);
        final ResourceHandler resourceHandler = new ResourceHandler()
                .setResourceManager(resourceManager)
                .setDirectoryListingEnabled(false);

        return new PredicateHandler(new Predicate() {
                @Override
                public boolean resolve(HttpServerExchange value) {
                    try {
                        return value.getRelativePath().length() > 0 &&
                                !value.getRelativePath().equals("/") &&
                                resourceManager.getResource(value.getRelativePath()) != null;
                    } catch (IOException ex) {
                        return false;
                    }
                }
        }, resourceHandler, baseHandler);
    }

    private final String name;
    private int port;
    private String host;
    private Undertow undertow;
    private PathHandler pathHandler = new PathHandler();
    private boolean started;

    protected  final Map<String, Runnable> contextRegistrar = new HashMap<>();

    private static final Logger log = Logger.getLogger(Web.class);
}
