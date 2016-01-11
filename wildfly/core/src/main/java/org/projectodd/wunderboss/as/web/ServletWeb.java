/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.as.web;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.CompletableFuture;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.as.ActionConduit;
import org.projectodd.wunderboss.web.Web;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.projectodd.wunderboss.web.Web.RegisterOption.PATH;
import static org.projectodd.wunderboss.web.Web.RegisterOption.SERVLET_NAME;

public class ServletWeb implements Web<Servlet> {

    public ServletWeb(String name, ServletContext servletContext,
                      ActionConduit actionConduit, AtomicLong sharedTimeout) {
        this.name = name;
        this.servletContext = servletContext;
        this.actionConduit = actionConduit;
        this.sharedTimeout = sharedTimeout;
    }

    @Override
    public boolean registerHandler(Servlet handler, Map<RegisterOption, Object> opts) {
        return registerServlet(handler, opts);
    }

    @Override
    public boolean registerServlet(final Servlet servlet, Map<RegisterOption, Object> opts) {
        final Options<RegisterOption> options = new Options<>(opts);
        final String context = options.getString(PATH);
        final String servletName = options.getString(SERVLET_NAME, context);
        // TODO: Take mapping instead of path for servlets?
        final String mapping = context.endsWith("/") ? context + "*" : context + "/*";

        final CompletableFuture<Void> servletFuture = new CompletableFuture<>();
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                try {
                    ServletRegistration.Dynamic servletRegistration = servletContext.addServlet(servletName, servlet);
                    servletRegistration.addMapping(mapping);
                    servletRegistration.setLoadOnStartup(1);
                    servletRegistration.setAsyncSupported(true);
                    servletRegistration.setInitParameter(ORIGINAL_CONTEXT, context);

                    Map<String, Filter> filterMap = (Map<String, Filter>) options.get(RegisterOption.FILTER_MAP);
                    if (filterMap != null) {
                        for (Map.Entry<String, Filter> entry : filterMap.entrySet()) {
                            FilterRegistration.Dynamic filter = servletContext.addFilter(entry.getKey() + servletName, entry.getValue());
                            filter.setAsyncSupported(true);
                            filter.addMappingForUrlPatterns(null, false, mapping);
                        }
                    }

                    servletFuture.complete(null);
                } catch (Exception e) {
                    servletFuture.completeExceptionally(e);
                }
            }
        };

        if (!this.actionConduit.add(action)) {
            throw new IllegalStateException("Can't add servlet after servlet init has completed");
        }

        try {
            // this shares a timeout with the ServletListener so we're chipping
            // away at the same pool of time. It is responsible for noticing that
            // the full timeout has expired
            final long now = System.currentTimeMillis();
            servletFuture.get(this.sharedTimeout.get(), TimeUnit.MILLISECONDS);
            this.sharedTimeout.addAndGet(System.currentTimeMillis() - now);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Registering servlet failed", e);

            throw new RuntimeException(e instanceof ExecutionException ? e.getCause() : e);
        }

        return false;
    }

    @Override
    public boolean unregister(Map<RegisterOption, Object> opts) {
        log.warn("Removing a servlet is a no-op in container.");

        return false;
    }

    @Override
    public Set<String> registeredContexts() {
        Set<String> contexts = new HashSet<>();

        for(ServletRegistration each : this.servletContext.getServletRegistrations().values()) {
            contexts.add(each.getInitParameter(ORIGINAL_CONTEXT));
        }

        return Collections.unmodifiableSet(contexts);
    }

    @Override
    public void start() {
        // no-op on WildFly
    }

    @Override
    public void stop() {
        // no-op on WildFly
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public String name() {
        return this.name;
    }

    private final String name;
    private final ServletContext servletContext;
    private final ActionConduit actionConduit;
    private final AtomicLong sharedTimeout;

    private static final String ORIGINAL_CONTEXT = "original-context";
    private static final Logger log = Logger.getLogger(ServletWeb.class);

}
