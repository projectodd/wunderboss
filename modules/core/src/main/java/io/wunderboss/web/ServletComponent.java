package io.wunderboss.web;

import io.undertow.predicate.Predicate;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.wunderboss.Application;
import io.wunderboss.Component;
import io.wunderboss.ComponentInstance;
import io.wunderboss.Options;
import io.wunderboss.WunderBoss;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServletComponent extends Component {
    @Override
    public String[] getComponentDependencies() {
        return new String[]{"web"};
    }

    @Override
    public void boot() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void configure(Options options) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public ComponentInstance start(Application application, Options options) {
        String context = options.getString("context", "/");
        Class<Servlet> servletClass = application.coerceObjectToClass(options.get("servlet_class"), Class.class);
        final ServletInfo servlet = Servlets.servlet(servletClass.getSimpleName(), servletClass)
                .addMapping("/*");

        final DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(WunderBoss.class.getClassLoader())
                .setContextPath(context)
                .setDeploymentName(context)
                .addServlet(servlet);

        if (options.containsKey("static_dir")) {
            servletBuilder.setResourceManager(new CachingResourceManager(1000, 1L, null,
                    new FileResourceManager(new File(options.getString("static_dir")), 1 * 1024 * 1024), 250));

            servletBuilder.addInitialHandlerChainWrapper(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    final ResourceHandler resourceHandler = new ResourceHandler()
                            .setResourceManager(servletBuilder.getResourceManager())
                            .setDirectoryListingEnabled(false);

                    return new PredicateHandler(new Predicate() {
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
                }
            });
        }

        if (options.containsKey("context_attributes")) {
            Map<String, Object> contextAttributes = application
                    .coerceObjectToClass(options.get("context_attributes"), Map.class);
            for (Map.Entry<String, Object> entry : contextAttributes.entrySet()) {
                servletBuilder.addServletContextAttribute(entry.getKey(), entry.getValue());
            }
        }

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        try {
            Options webOptions = new Options();
            webOptions.put("context", context);
            webOptions.put("http_handler", manager.start());
            ComponentInstance web = application.start("web", webOptions);

            Options instanceOptions = new Options();
            instanceOptions.put("manager", manager);
            instanceOptions.put("web", web);
            return new ComponentInstance(this, instanceOptions);
        } catch (ServletException e) {
            // TODO: something better
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void stop(ComponentInstance instance) {
        ComponentInstance web = (ComponentInstance) instance.getOptions().get("web");
        DeploymentManager manager = (DeploymentManager) instance.getOptions().get("manager");

        try {
            DeploymentInfo deploymentInfo = manager.getDeployment().getDeploymentInfo();
            manager.stop();
            manager.undeploy();
            Servlets.defaultContainer().removeDeployment(deploymentInfo);
            web.stop();
        } catch (ServletException e) {
            // TODO: something better
            e.printStackTrace();
        }
    }
}
