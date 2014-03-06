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

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WebComponent extends Component<Undertow> {
    @Override
    public void start() {
        // TODO: Configurable non-lazy boot of Undertow
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
    public Undertow backingObject() {
        return this.undertow;
    }

    @Override
    protected void configure(Options options) {
        port = options.getInt("port", 8080);
        host = options.getString("host", "localhost");
        undertow = Undertow.builder()
                .addListener(port, host)
                .setHandler(Handlers.date(Handlers.header(pathHandler, Headers.SERVER_STRING, "undertow")))
                .build();
    }

    public void registerHttpHandler(String context, HttpHandler httpHandler, Options options) {
        if (options != null &&
                options.containsKey("static_dir")) {
            httpHandler = wrapWithStaticHandler(httpHandler, options.getString("static_dir"));
        }
        pathHandler.addPrefixPath(context, httpHandler);
        if (!started) {
            undertow.start();
            log.info("Undertow listening on " + host + ":" + port);
            started = true;
        }
        log.info("Started web context " + context);
    }

    public void unregisterHttpHandler(String context) {
        pathHandler.removePrefixPath(context);
        log.info("Stopped web context " + context);
    }

    public void registerServlet(String context, Class servletClass, Options options) {
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

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        try {
            Options webOptions = new Options();
            if (options.containsKey("static_dir")) {
                webOptions.put("static_dir", options.getString("static_dir"));
            }
            registerHttpHandler(context, manager.start(), webOptions);
            deploymentManagers.put(context, manager);
        } catch (ServletException e) {
            // TODO: something better
            e.printStackTrace();
        }
    }

    public void unregisterServlet(String context) {
        DeploymentManager manager = this.deploymentManagers.get(context);

        //TODO: handle case when servlet does not exist
        try {
            DeploymentInfo deploymentInfo = manager.getDeployment().getDeploymentInfo();
            manager.stop();
            manager.undeploy();
            Servlets.defaultContainer().removeDeployment(deploymentInfo);
        } catch (ServletException e) {
            // TODO: something better
            e.printStackTrace();
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

    private int port;
    private String host;
    private Undertow undertow;
    private PathHandler pathHandler = new PathHandler();
    private boolean started;
    private final Map<String, DeploymentManager> deploymentManagers = new HashMap<>();

    private static final Logger log = Logger.getLogger(WebComponent.class);
}
