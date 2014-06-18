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

import io.undertow.server.HttpHandler;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.web.UndertowWeb;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.projectodd.wunderboss.web.Web.RegisterOption.*;

public class WildFlyWeb extends UndertowWeb {

    public WildFlyWeb(String name, UndertowService undertowService) {
        super(name, new Options<CreateOption>());
        this.undertowService = undertowService;
    }

    @Override
    public boolean registerHandler(HttpHandler httpHandler, Map<RegisterOption, Object> opts) {
        final Options<RegisterOption> options = new Options<>(opts);
        final String context = options.getString(PATH);

        if (options.has(STATIC_DIR)) {
            httpHandler = wrapWithStaticHandler(httpHandler, options.getString(STATIC_DIR));
        }
        for (Host host : getHosts()) {
            log.info("Registered HTTP context '" + context + "' for host " + host.getName());
            host.registerHandler(context, httpHandler);
        }
        // TODO: not sure how to handle vhosts here
        final boolean replacement = register(context, null, httpHandler);
        epilogue(httpHandler, new Runnable() {
            public void run() {
                for (Host host : getHosts()) {
                    host.unregisterHandler(context);
                }
            }
        });
        return replacement;
    }

    @Override
    public void start() {
        // no-op on WildFly
    }

    @Override
    public void stop() {
        // no-op on WildFly
    }

    private List<Host> getHosts() {
        List<Host> hosts = new ArrayList<Host>();
        for (Server server : undertowService.getServers()) {
            for (Host host : server.getHosts()) {
                hosts.add(host);
            }
        }
        return hosts;
    }

    private UndertowService undertowService;

    private static final Logger log = Logger.getLogger(WildFlyWeb.class);
}
