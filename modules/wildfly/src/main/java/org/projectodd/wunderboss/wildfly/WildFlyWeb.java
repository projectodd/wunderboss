/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss.wildfly;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.ServletContextImpl;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.web.UndertowWeb;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.Map;

import static org.projectodd.wunderboss.web.Web.RegisterOption.PATH;

public class WildFlyWeb extends UndertowWeb {

    public WildFlyWeb(String name, ServletContext servletContext) {
        super(name, new Options<CreateOption>());
        this.servletContext = servletContext;
        if (servletContext != null) {
            addHandlerWrapper();
        }
    }

    @Override
    public boolean registerServlet(Servlet servlet, Map<RegisterOption, Object> opts) {
        final Options<RegisterOption> options = new Options<>(opts);
        final String context = options.getString(PATH);
        // TODO: Take mapping instead of path for servlets?
        final String mapping = context.endsWith("/") ? context + "*" : context + "/*";

        ServletRegistration.Dynamic servletRegistration = servletContext.addServlet(context, servlet);
        servletRegistration.addMapping(mapping);
        servletRegistration.setLoadOnStartup(1);
        servletRegistration.setAsyncSupported(true);

        HttpHandler handler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                // Restore the relative and resolved paths we saved earlier
                // so that the Servlet mapping will match correctly
                final String originalRelativePath = exchange.getPathParameters().get(ORIGINAL_RELATIVE_PATH).remove();
                final String originalResolvedPath = exchange.getPathParameters().get(ORIGINAL_RESOLVED_PATH).remove();
                exchange.setRelativePath(originalRelativePath);
                exchange.setResolvedPath(originalResolvedPath);
                servletHandler.handleRequest(exchange);
            }
        };
        return registerHandler(handler, options);
    }

    @Override
    public void start() {
        // no-op on WildFly
    }

    @Override
    public void stop() {
        // no-op on WildFly
    }

    private void addHandlerWrapper() {
        ServletContextImpl servletContextImpl = (ServletContextImpl) servletContext;
        // Hand off all requests to our own path matching logic
        servletContextImpl.getDeployment().getDeploymentInfo().addInitialHandlerChainWrapper(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                servletHandler = handler;
                return new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        // Save off the relative and resolved paths in case
                        // this ends up being handled by a servlet
                        exchange.addPathParam(ORIGINAL_RELATIVE_PATH, exchange.getRelativePath());
                        exchange.addPathParam(ORIGINAL_RESOLVED_PATH, exchange.getResolvedPath());
                        pathology.handler().handleRequest(exchange);
                    }
                };
            }
        });
    }

    private ServletContext servletContext;
    private HttpHandler servletHandler;

    private static final String ORIGINAL_RELATIVE_PATH = "wunderboss.wildfly.orig_relative_path";
    private static final String ORIGINAL_RESOLVED_PATH = "wunderboss.wildfly.orig_resolved_path";
    private static final Logger log = Logger.getLogger(WildFlyWeb.class);
}
