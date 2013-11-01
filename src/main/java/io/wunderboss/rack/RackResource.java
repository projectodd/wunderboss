package io.wunderboss.rack;

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
import org.jboss.logging.Logger;
import org.jruby.runtime.builtin.IRubyObject;
import org.projectodd.restafari.spi.InitializationException;
import org.projectodd.restafari.spi.RequestContext;
import org.projectodd.restafari.spi.ResourceContext;
import org.projectodd.restafari.spi.resource.Resource;
import org.projectodd.restafari.spi.resource.RootResource;
import org.projectodd.restafari.spi.resource.async.CollectionResource;
import org.projectodd.restafari.spi.resource.async.ResourceSink;
import org.projectodd.restafari.spi.resource.async.Responder;
import org.projectodd.restafari.spi.state.ObjectResourceState;
import org.projectodd.restafari.spi.state.PropertyResourceState;
import org.projectodd.restafari.spi.state.ResourceState;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class RackResource implements CollectionResource, RootResource {

    public RackResource(String id) {
        this.id = id;
        this.pathHandler = new PathHandler();
    }

    @Override
    public void initialize(ResourceContext context) throws InitializationException {
        String host = context.config().getRequired("host");
        int port = context.config().getRequired("port");
        this.undertow = Undertow.builder()
                .addListener(port, host)
                .setHandler(this.pathHandler)
                .build();
        this.undertow.start();
        log.info("WunderBoss listening on " + host + ":" + port);
    }

    @Override
    public void destroy() {
        this.undertow.stop();
        log.info("WunderBoss stopped");
    }

    @Override
    public void create(RequestContext ctx, ResourceState state, Responder responder) {
        if (state instanceof ObjectResourceState) {
            ObjectResourceState objectState = (ObjectResourceState) state;
            String context = objectState.id();
            String rackRoot = (String) objectState.getProperty("root");
            IRubyObject rackApp = (IRubyObject) objectState.getProperty("rackApp");

            final ServletInfo servlet = Servlets.servlet("RackServlet", RackServlet.class)
                    .addMapping("/*");

            final DeploymentInfo servletBuilder = Servlets.deployment()
                    .setClassLoader(RackResource.class.getClassLoader())
                    .setContextPath(context)
                    .setDeploymentName(context)
                    .addServlet(servlet)
                    .addServletContextAttribute("rack_application", rackApp)
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
            try {
                this.pathHandler.addPath(context, manager.start());
                RackApplicationResource rackAppResource = new RackApplicationResource(this, context);
                this.collection.put(context, rackAppResource);
                responder.resourceCreated(rackAppResource);
            } catch (ServletException e) {
                log.error("Error deploying Rack application", e);
                responder.internalError(e.getMessage());
            }
        } else {
            // TODO: How do we only support creating Objects, not Collections?
            responder.createNotSupported(this);
        }
    }

    @Override
    public void delete(RequestContext ctx, Responder responder) {
        responder.deleteNotSupported( this );
    }

    @Override
    public void readContent(RequestContext ctx, ResourceSink sink) {
        Stream<Resource> stream = this.collection.values().stream();
        stream.forEach((m) -> {
            sink.accept(m);
        });
        sink.close();
    }

    @Override
    public void read(RequestContext ctx, String id, Responder responder) {
        if (this.collection.containsKey(id)) {
            responder.resourceRead(this.collection.get(id));
        } else {
            responder.noSuchResource( id );
        }
    }

    @Override
    public String id() {
        return this.id;
    }

    private String id;
    private PathHandler pathHandler;
    private Undertow undertow;
    private Map<String, Resource> collection = new LinkedHashMap<>();

    private static final Logger log = Logger.getLogger(RackResource.class);
}
