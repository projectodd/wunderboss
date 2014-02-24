package org.projectodd.wunderboss.wildfly;

import io.undertow.server.HttpHandler;
import org.jboss.msc.service.ServiceRegistry;
import org.projectodd.wunderboss.web.WebComponent;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowService;

public class WildFlyWebComponent extends WebComponent {

    public WildFlyWebComponent(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    protected void registerHttpHandler(String context, HttpHandler httpHandler) {
        getHost().registerHandler(context, httpHandler);
    }

    @Override
    protected void unregisterHttpHandler(String context) {
        getHost().unregisterHandler(context);
    }

    private Host getHost() {
        UndertowService undertowService = (UndertowService) serviceRegistry.getRequiredService(UndertowService.UNDERTOW).getValue();
        String defaultServerName = undertowService.getDefaultServer();
        String defaultVirtualHost = undertowService.getDefaultVirtualHost();
        return (Host) serviceRegistry.getRequiredService(UndertowService.virtualHostName(defaultServerName, defaultVirtualHost)).getValue();
    }

    private ServiceRegistry serviceRegistry;
}
