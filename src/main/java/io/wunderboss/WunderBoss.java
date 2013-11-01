package io.wunderboss;

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
import io.wunderboss.rack.RackServlet;
import org.jboss.logging.Logger;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.File;
import java.io.IOException;

public class WunderBoss {

    public WunderBoss() {
        pathHandler = new PathHandler();
    }

    public void deploy_rack_application(String context, IRubyObject app) throws Exception {
        final ServletInfo servlet = Servlets.servlet("RackServlet", RackServlet.class)
                .addMapping("/*");

        final DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(WunderBoss.class.getClassLoader())
                .setContextPath(context)
                .setDeploymentName(context)
                .addServlet(servlet)
                .addServletContextAttribute("rack_application", app)
                .setResourceManager(new CachingResourceManager(1000, 1L, null, new FileResourceManager(new File("public/"), 1 * 1024 * 1024), 250));

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
    }

    public void start(String host, int port) {
        if (undertow != null) {
            throw new RuntimeException("WunderBoss already started");
        }

        undertow = Undertow.builder()
                .addListener(port, host)
                .setHandler(pathHandler)
                .build();
        
        undertow.start();
        
        log.info("WunderBoss listening on " + host + ":" + port);
    }

    public void stop() {
        if (undertow != null) {
            undertow.stop();
            log.info("WunderBoss stopped");
        }
    }

    private Undertow undertow;
    private PathHandler pathHandler;

    private static final Logger log = Logger.getLogger(WunderBoss.class);
}
