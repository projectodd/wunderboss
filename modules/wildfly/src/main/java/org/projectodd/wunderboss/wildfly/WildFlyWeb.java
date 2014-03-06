package org.projectodd.wunderboss.wildfly;

import io.undertow.server.HttpHandler;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.web.Web;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

import java.util.ArrayList;
import java.util.List;

public class WildFlyWeb extends Web {

    public WildFlyWeb(String name, UndertowService undertowService) {
        super(name, null);
        this.undertowService = undertowService;
    }

    @Override
    public void registerHttpHandler(String context, HttpHandler httpHandler, Options options) {
        if (options != null &&
                options.containsKey("static_dir")) {
            httpHandler = wrapWithStaticHandler(httpHandler, options.getString("static_dir"));
        }
        for (Host host : getHosts()) {
            log.info("Registered HTTP context '" + context + "' for host " + host.getName());
            host.registerHandler(context, httpHandler);
        }
    }

    @Override
    public void unregisterHttpHandler(String context) {
        for (Host host : getHosts()) {
            log.info("Unregistered HTTP context '" + context + "' for host " + host.getName());
            host.unregisterHandler(context);
        }
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
