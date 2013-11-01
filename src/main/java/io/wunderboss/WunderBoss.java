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
import io.wunderboss.rack.RackResource;
import io.wunderboss.rack.RackServlet;
import org.jboss.logging.Logger;
import org.jruby.runtime.builtin.IRubyObject;
import org.projectodd.restafari.container.DefaultContainer;
import org.projectodd.restafari.container.DirectConnector;
import org.projectodd.restafari.container.SimpleConfig;
import org.projectodd.restafari.container.UnsecureServer;
import org.projectodd.restafari.container.codec.DefaultObjectResourceState;
import org.projectodd.restafari.container.resource.ContainerResource;
import org.projectodd.restafari.spi.resource.Resource;
import org.projectodd.restafari.spi.state.ObjectResourceState;

import java.io.File;
import java.io.IOException;

public class WunderBoss {

    public WunderBoss(String host, int port) throws Exception {
        this.container = new DefaultContainer();
        this.container.registerResource(new ContainerResource("_container"), new SimpleConfig());

        SimpleConfig rackConfig = new SimpleConfig();
        rackConfig.put("host", host);
        rackConfig.put("port", port);
        this.rackResource = new RackResource("rack");
        this.container.registerResource(rackResource, rackConfig);

        int restafariPort = port + 1;
        this.restafariServer = new UnsecureServer(this.container, host, restafariPort);
        this.restafariServer.start();
        log.info("Restafari listening on " + host + ":" + restafariPort);
    }

    public void deploy_rack_application(String context, IRubyObject app) throws Exception {
        ObjectResourceState state = new DefaultObjectResourceState(context);
        state.addProperty("root", ".");
        state.addProperty("rackApp", app);
        this.container.directConnector().create("/rack", state);
    }

    public void stop() throws Exception {
        // TODO: How do we unregister resources from the container?
        this.rackResource.destroy();

        if (this.restafariServer != null) {
            this.restafariServer.stop();
            log.info("Restafari stopped");
        }
    }

    private DefaultContainer container;
    private RackResource rackResource;
    private UnsecureServer restafariServer;

    private static final Logger log = Logger.getLogger(WunderBoss.class);
}