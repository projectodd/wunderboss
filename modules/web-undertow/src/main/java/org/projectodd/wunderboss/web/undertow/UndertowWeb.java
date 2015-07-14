/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.web.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.NameVirtualHostHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.util.Headers;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.web.Web;
import org.slf4j.Logger;
import org.xnio.OptionMap;
import org.xnio.Xnio;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class UndertowWeb implements Web<HttpHandler> {

    public UndertowWeb(String name, Options<CreateOption> opts) {
        this.name = name;
        this.sessionManager = new InMemorySessionManager(name+"session-manager", -1);
        configure(opts);
    }

    @Override
    public String name() { return name; }

    @Override
    public void start() {
        // TODO: Configurable non-lazy boot of Undertow
        if (!started) {
            undertow.start();
            started = true;
        }
    }

    @Override
    public void stop() {
        if (started) {
            undertow.stop();
            started = false;
        }
    }

    @Override
    public boolean isRunning() {
        return started;
    }

    public Undertow undertow() {
        return this.undertow;
    }

    private void configure(Options<CreateOption> options) {
        autoStart = options.getBoolean(CreateOption.AUTO_START);
        Undertow.Builder builder = (Undertow.Builder) options.get(CreateOption.CONFIGURATION);
        if (builder != null) {
            undertow = builder
                .setHandler(Handlers.header(pathology.handler(), Headers.SERVER_STRING, "undertow"))
                .build();
        } else {
            int port = options.getInt(CreateOption.PORT);
            String host = options.getString(CreateOption.HOST);
            undertow = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(Handlers.header(pathology.handler(), Headers.SERVER_STRING, "undertow"))
                .build();
        }
    }

    @Override
    public boolean registerHandler(HttpHandler httpHandler, Map<RegisterOption, Object> opts) {
        return registerHandler(httpHandler, opts, null);
    }

    protected boolean registerHandler(HttpHandler httpHandler, Map<RegisterOption, Object> opts, Runnable cleanup) {
        final Options<RegisterOption> options = new Options<>(opts);
        final String context = options.getString(RegisterOption.PATH);

        httpHandler = wrapWithSessionHandler(httpHandler);
        if (options.has(RegisterOption.STATIC_DIR)) {
            httpHandler = wrapWithStaticHandler(httpHandler, options.getString(RegisterOption.STATIC_DIR));
        }
        if (options.getBoolean(RegisterOption.DISPATCH)) {
            httpHandler = wrapWithDispatcher(httpHandler);
        }
        final boolean replacement = pathology.add(context, options.getList(RegisterOption.VHOSTS), httpHandler);
        if (cleanup != null) {
            pathology.epilogue(httpHandler, cleanup);
        }
        if (autoStart) {
            start();
        }
        log.info("Registered web context {}", context);

        return replacement;
    }

    @Override
    public boolean registerServlet(Servlet servlet, Map<RegisterOption, Object> opts) {
        final Options<RegisterOption> options = new Options<>(opts);
        final String context = options.getString(RegisterOption.PATH);
        final String servletName = options.getString(RegisterOption.SERVLET_NAME);

        Class servletClass = servlet.getClass();
        final ServletInfo servletInfo = Servlets.servlet(servletName != null ? servletName : servletClass.getSimpleName(),
                                                         servletClass,
                                                         new ImmediateInstanceFactory(servlet));
        servletInfo.addMapping("/*");
        // LoadOnStartup is required for any websocket Endpoints to work
        servletInfo.setLoadOnStartup(1);
        // Support async servlets
        servletInfo.setAsyncSupported(true);

        final DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(WunderBoss.class.getClassLoader())
                .setContextPath("/".equals(context) ? "" : context)
                // actually flush the response when we ask for it
                .setIgnoreFlush(false)
                .setDeploymentName(UUID.randomUUID().toString())
                .addServlet(servletInfo);

        // Required for any websocket support in undertow
        final WebSocketDeploymentInfo wsInfo = new WebSocketDeploymentInfo();

        // without a worker, undertow complains with:
        // UT026009: XNIO worker was not set on WebSocketDeploymentInfo, web socket client will not be available.
        // so we give it a basic one, since we can't seem to get one from elsewhere.
        // TODO: figure out if this is really the right thing to do
        try {
            wsInfo.setWorker(Xnio.getInstance().createWorker(OptionMap.create(org.xnio.Options.THREAD_DAEMON, true)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        servletBuilder.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsInfo);

        final DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        boolean replacement = false;
        try {
            HttpHandler handler = manager.start();
            replacement = registerHandler(handler, options, new Runnable() {
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

        return replacement;
    }

    @Override
    public boolean unregister(Map<RegisterOption, Object> opts) {
        final Options<RegisterOption> options = new Options<>(opts);
        return pathology.remove(options.getString(RegisterOption.PATH), options.getList(RegisterOption.VHOSTS));
    }

    @Override
    public Set<String> registeredContexts() {
        return Collections.unmodifiableSet(pathology.getActiveHandlers());
    }

    // For the WF subclass
    protected boolean register(String path, List<String> vhosts, HttpHandler handler) {
        return pathology.add(path, vhosts, handler);
    }

    protected HttpHandler wrapWithSessionHandler(HttpHandler handler) {
        return new SessionAttachmentHandler(handler, sessionManager, new SessionCookieConfig());
    }

    protected HttpHandler wrapWithStaticHandler(HttpHandler baseHandler, String path) {
        // static path is given relative to application root
        if (!new File(path).isAbsolute()) {
            path = WunderBoss.options().get("root", "").toString() + File.separator + path;
        }
        if (!new File(path).exists()) {
            log.debug("Not adding static handler for nonexistent directory {}", path);
            return baseHandler;
        }
        log.debug("Adding static handler for {}", path);
        final ResourceManager resourceManager =
                new CachingResourceManager(1000, 1L, null,
                                           new FileResourceManager(new File(path), 1 * 1024 * 1024), 250);
        String[] welcomeFiles = new String[] { "index.html", "index.html", "default.html", "default.htm" };
        final List<String> welcomeFileList = new CopyOnWriteArrayList<>(welcomeFiles);
        final ResourceHandler resourceHandler = new ResourceHandler()
                .setResourceManager(resourceManager)
                .setWelcomeFiles(welcomeFiles)
                .setDirectoryListingEnabled(false);

        return new PredicateHandler(new Predicate() {
                @Override
                public boolean resolve(HttpServerExchange value) {
                    try {
                        Resource resource = resourceManager.getResource(value.getRelativePath());
                        if (resource == null) {
                            return false;
                        }
                        if (resource.isDirectory()) {
                            Resource indexResource = getIndexFiles(resourceManager, resource.getPath(), welcomeFileList);
                            return indexResource != null;
                        }
                        return true;
                    } catch (IOException ex) {
                        return false;
                    }
                }
        }, resourceHandler, baseHandler);
    }

    /*
     * Ensure that handler isn't invoked on IO thread but rather
     * dispatched to the worker thread pool
     */
    protected HttpHandler wrapWithDispatcher(final HttpHandler handler) {
        return new HttpHandler() {
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                if (exchange.isInIoThread()) {
                    exchange.dispatch(handler);
                } else {
                    handler.handleRequest(exchange);
                }
            }
        };
    }

    protected Resource getIndexFiles(ResourceManager resourceManager, final String base, List<String> possible) throws IOException {
        String realBase;
        if (base.endsWith("/")) {
            realBase = base;
        } else {
            realBase = base + "/";
        }
        for (String possibility : possible) {
            Resource index = resourceManager.getResource(realBase + possibility);
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private final String name;
    private Undertow undertow;
    private boolean autoStart;
    private SessionManager sessionManager;
    protected Pathology pathology = new Pathology();
    private boolean started;
    private Map<String, Runnable> contextRegistrar = new HashMap<>();

    private static final Logger log = WunderBoss.logger(Web.class);

    public static class Pathology {

        public Pathology() {
            vhostHandler = new NameVirtualHostHandler();
            pathHandler = new PathHandler();
            vhostHandler.setDefaultHandler(pathHandler);
        }

        public HttpHandler handler() {
            return vhostHandler;
        }

        public synchronized boolean add(String path, List<String> vhosts, HttpHandler handler) {
            boolean result = false;
            if (vhosts == null) {
                result = null != activeHandlers.put(path, handler);
                pathHandler.addPrefixPath(path, handler);
            } else {
                for(String host: vhosts) {
                    result = (null != activeHandlers.put(host + path, handler)) || result;
                    PathHandler ph = (PathHandler) vhostHandler.getHosts().get(host);
                    if (ph == null) {
                        ph = new PathHandler();
                        vhostHandler.addHost(host, ph);
                    }
                    ph.addPrefixPath(path, handler);
                }
            }
            purge();
            return result;
        }

        public synchronized boolean remove(String path, List<String> vhosts) {
            boolean result = false;
            if (vhosts == null) {
                result = null != activeHandlers.remove(path);
                pathHandler.removePrefixPath(path);
            } else {
                for(String host: vhosts) {
                    if (null != activeHandlers.remove(host + path)) {
                        result = true;
                        PathHandler ph = (PathHandler) vhostHandler.getHosts().get(host);
                        ph.removePrefixPath(path);
                    }
                }
            }
            purge();
            return result;
        }

        public void epilogue(HttpHandler handler, Runnable f) {
            epilogues.put(handler, f);
        }

        public Set<String> getActiveHandlers() {
            return activeHandlers.keySet();
        }

        private void purge() {
            Set<HttpHandler> keys = new HashSet<>(epilogues.keySet());
            keys.removeAll(activeHandlers.values());
            for(HttpHandler handler: keys) {
                epilogues.remove(handler).run();
            }
        }

        private NameVirtualHostHandler vhostHandler;
        private PathHandler pathHandler;
        private Map<String, HttpHandler> activeHandlers = new HashMap<>();
        private Map<HttpHandler, Runnable> epilogues = new HashMap<>();
    }

}
