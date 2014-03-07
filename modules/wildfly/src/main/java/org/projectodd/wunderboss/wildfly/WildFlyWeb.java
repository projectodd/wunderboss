package org.projectodd.wunderboss.wildfly;

import io.undertow.server.HttpHandler;
import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.web.Web;
import org.projectodd.wunderboss.web.UndertowWeb;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WildFlyWeb extends UndertowWeb {

    public WildFlyWeb(String name, UndertowService undertowService) {
        super(name, new Options());
        this.undertowService = undertowService;
    }

    @Override
    public Web registerHandler(HttpHandler httpHandler, Map<String, Object> opts) {
        final Options options = new Options(opts);
        final String context = getContextPath(options);
        if (options.containsKey("static_dir")) {
            httpHandler = wrapWithStaticHandler(httpHandler, options.getString("static_dir"));
        }
        for (Host host : getHosts()) {
            log.info("Registered HTTP context '" + context + "' for host " + host.getName());
            host.registerHandler(context, httpHandler);
        }
        epilogue(options, new Runnable() { 
                public void run() { 
                    for (Host host : getHosts()) {
                        host.unregisterHandler(context);
                    }}});
        return this;
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
