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
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Wundertow {

    public Wundertow(Map<String, String> config) {
        port = Integer.parseInt(config.get("web_port"));
        host = config.get("web_host");
        undertow = Undertow.builder()
                .addListener(port, host)
                .setHandler(pathHandler)
                .build();
    }

    public synchronized void stop() {
        if (started) {
            undertow.stop();
            log.info("Wundertop stopped");
            started = false;
        }
    }

    public synchronized void deployServlet(String context, Class<? extends Servlet> servletClass,
                                           Map<String, Object> servletContextAttributes,
                                           Map<String, String> config) throws Exception {
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
                            if (value.getRelativePath().length() > 0 && !value.getRelativePath().equals("/")) {
                                return servletBuilder.getResourceManager().getResource(value.getRelativePath()) != null;
                            }
                            return false;
                        } catch (IOException ex) {
                            return false;
                        }
                    }
                }, resourceHandler, handler);

                return predicateHandler;
            }
        });


        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        deploymentManagers.put(context, manager);
        manager.deploy();
        pathHandler.addPath(context, manager.start());

        if (!started) {
            undertow.start();
            log.info("Wundertow listening on " + host + ":" + port);
            started = true;
        }
    }

    public synchronized void undeploy(String context) throws ServletException {
        DeploymentManager manager = deploymentManagers.remove(context);
        if (manager != null) {
            manager.stop();
            manager.undeploy();
            Servlets.defaultContainer().removeDeployment(manager.getDeployment().getDeploymentInfo());
        }
        pathHandler.removePath(context);
    }

    private static final Logger log = Logger.getLogger(Wundertow.class);

    private Undertow undertow;
    private boolean started = false;
    private PathHandler pathHandler = new PathHandler();
    private Map<String, DeploymentManager> deploymentManagers = new HashMap<>();
    private int port;
    private String host;
}
