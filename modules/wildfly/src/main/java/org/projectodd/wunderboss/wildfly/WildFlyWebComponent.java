package org.projectodd.wunderboss.wildfly;

import io.undertow.server.HttpHandler;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.web.WebComponent;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

import java.util.ArrayList;
import java.util.List;

public class WildFlyWebComponent extends WebComponent {

    public WildFlyWebComponent(UndertowService undertowService) {
        this.undertowService = undertowService;
    }

    @Override
    public void configure(Options options) {
        // no-op since we're using undertow from wildfly
    }

    @Override
    protected void registerHttpHandler(String context, HttpHandler httpHandler) {
        for (Host host : getHosts()) {
            log.info("Registered HTTP context '" + context + "' for host " + host.getName());
            host.registerHandler(context, httpHandler);
        }
    }

    @Override
    protected void unregisterHttpHandler(String context) {
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

    private static final Logger log = Logger.getLogger(WildFlyWebComponent.class);
}
