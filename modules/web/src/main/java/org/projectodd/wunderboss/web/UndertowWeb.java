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
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.util.Headers;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.projectodd.wunderboss.web.Web.CreateOption.*;
import static org.projectodd.wunderboss.web.Web.RegisterOption.*;

public class UndertowWeb implements Web<HttpHandler> {

    public UndertowWeb(String name, Options<CreateOption> opts) {
        this.name = name;
        configure(opts);
    }

    @Override
    public String name() { return name; }

    @Override
    public void start() {
        // TODO: Configurable non-lazy boot of Undertow
        if (!started) {
            undertow.start();
            log.infof("Undertow listening on %s:%s", host, port);
            started = true;
        }
    }

    @Override
    public void stop() {
        if (started) {
            undertow.stop();
            log.info("Undertow stopped");
            started = false;
        }
    }

    public Undertow undertow() {
        return this.undertow;
    }

    private void configure(Options<CreateOption> options) {
        autoStart = options.getBoolean(AUTO_START, (Boolean)AUTO_START.defaultValue);
        port = options.getInt(PORT, (Integer)PORT.defaultValue);
        host = options.getString(HOST, (String)HOST.defaultValue);
        undertow = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(Handlers.header(pathHandler, Headers.SERVER_STRING, "undertow"))
                .build();
    }

    public boolean registerHandler(HttpHandler httpHandler, Map<RegisterOption, Object> opts) {
        final Options<RegisterOption> options = new Options<>(opts);
        final String context = getContextPath(options);
        final boolean replacement = hasContext(context);

        if (options.has(STATIC_DIR)) {
            httpHandler = wrapWithStaticHandler(httpHandler, options.getString(STATIC_DIR));
        }
        pathHandler.addPrefixPath(context, httpHandler);
        epilogue(options, new Runnable() { 
                public void run() { 
                    pathHandler.removePrefixPath(context);
                }});

        if (autoStart) {
            start();
        }
        log.infof("Registered web context %s", context);

        return replacement;
    }

    public boolean registerServlet(Servlet servlet, Map<RegisterOption, Object> opts) {
        final Options<RegisterOption> options = new Options<>(opts);
        final String context = getContextPath(options);
        final boolean replacement = hasContext(context);

        Class servletClass = servlet.getClass();
        final ServletInfo servletInfo = Servlets.servlet(servletClass.getSimpleName(),
                                                         servletClass,
                                                         new ImmediateInstanceFactory(servlet));
        servletInfo.addMapping("/*");
        // LoadOnStartup is required for any websocket Endpoints to work
        servletInfo.setLoadOnStartup(1);
        // Support async servlets
        servletInfo.setAsyncSupported(true);

        final DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(WunderBoss.class.getClassLoader())
                .setContextPath(context)
                // actually flush the response when we ask for it
                .setIgnoreFlush(false)
                .setDeploymentName(context)
                .addServlet(servletInfo);

        // Required for any websocket support in undertow
        final WebSocketDeploymentInfo wsInfo = new WebSocketDeploymentInfo();
        servletBuilder.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsInfo);

        final DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        try {
            registerHandler(manager.start(), options);
            epilogue(options, new Runnable() { 
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

    public boolean unregister(String context) {
        final Runnable f = contextRegistrar.remove(context);
        boolean exists = false;
        if (f != null) {
            f.run();
            exists = true;
            log.infof("Unregistered web context at path %s", context);
        } else {
            log.warnf("No context registered at path %s", context);
        }

        return exists;
    }

    public Set<String> registeredContexts() {
        return Collections.unmodifiableSet(this.contextRegistrar.keySet());
    }

    protected boolean hasContext(String context) {
        return this.contextRegistrar.keySet().contains(context);
    }

    /**
     * Associate a resource cleanup function with a context path,
     * invoked in the unregister method. The context is obtained from
     * the passed options.
     */
    protected void epilogue(Options<RegisterOption> options, final Runnable cleanup) {
        String context = getContextPath(options);
        contextRegistrar.put(context, cleanup);
    }

    protected HttpHandler wrapWithStaticHandler(HttpHandler baseHandler, String path) {
        // static path is given relative to application root
        if (!new File(path).isAbsolute()) {
            path = WunderBoss.options().get("root", "").toString() + File.separator + path;
        }
        if (!new File(path).exists()) {
            log.debugf("Not adding static handler for nonexistent directory %s", path);
            return baseHandler;
        }
        log.debugf("Adding static handler for %s", path);
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

    protected static String getContextPath(Options<RegisterOption> options) {
        return options.getString(PATH, (String)PATH.defaultValue);
    }
    
    private final String name;
    private int port;
    private String host;
    private Undertow undertow;
    private boolean autoStart;
    private PathHandler pathHandler = new PathHandler();
    private boolean started;
    private Map<String, Runnable> contextRegistrar = new HashMap<>();

    private static final Logger log = Logger.getLogger(Web.class);
}
