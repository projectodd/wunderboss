package io.wunderboss.web;

import io.undertow.Undertow;
import io.undertow.predicate.Predicate;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.wunderboss.WunderBoss;
import org.jboss.logging.Logger;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Wundertow {

    public Wundertow(Map<String, String> config) {
        this.port = Integer.parseInt(config.get("web_port"));
        this.host = config.get("web_host");
        this.undertow = Undertow.builder()
                .addListener(this.port, this.host)
                .setHandler(this.pathHandler)
                .build();
    }

    public synchronized void stop() {
        if (this.started) {
            this.undertow.stop();
            log.info("Wundertop stopped");
            this.started = false;
        }
    }

    public synchronized void deploy(Class<? extends Servlet> servletClass,
                                    Map<String, Object> servletContextAttributes,
                                    Map<String, String> config) throws Exception {
        String context = config.get("context");
        final ServletInfo servlet = Servlets.servlet(servletClass.getSimpleName(), servletClass)
                .addMapping("/*");

        final DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(WunderBoss.class.getClassLoader())
                .setContextPath(context)
                .setDeploymentName(context)
                .addServlet(servlet);

        if (config.containsKey("static_dir")) {
            servletBuilder.setResourceManager(new CachingResourceManager(1000, 1L, null,
                    new FileResourceManager(new File(config.get("static_dir")), 1 * 1024 * 1024), 250));
        }
        
        for (Map.Entry<String, Object> entry : servletContextAttributes.entrySet()) {
            servletBuilder.addServletContextAttribute(entry.getKey(), entry.getValue());
        }

        servletBuilder.addInitialHandlerChainWrapper(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler handler) {
                final ResourceHandler resourceHandler = new ResourceHandler()
                        .setResourceManager(servletBuilder.getResourceManager())
                        .setDirectoryListingEnabled(false);

                PredicateHandler predicateHandler = new PredicateHandler(new Predicate() {
                    @Override
                    public boolean resolve(HttpServerExchange value) {
                        try {
                            return servletBuilder.getResourceManager().getResource(value.getRelativePath()) != null;
                        } catch (IOException ex) {
                            return false;
                        }
                    }
                }, resourceHandler, handler);

                return predicateHandler;
            }
        });


        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        this.pathHandler.addPath(context, manager.start());

        if (!this.started) {
            this.undertow.start();
            log.info("Wundertow listening on " + host + ":" + port);
            this.started = true;
        }
    }

    private static final Logger log = Logger.getLogger(Wundertow.class);

    private Undertow undertow;
    private boolean started = false;
    private PathHandler pathHandler = new PathHandler();
    private int port;
    private String host;
}
