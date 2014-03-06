package org.projectodd.wunderboss.wildfly;

import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.ComponentProvider;
import org.wildfly.extension.undertow.UndertowService;

public class WildflyWebComponentProvider implements ComponentProvider {

    public WildflyWebComponentProvider(UndertowService service) {
        this.service = service;

    }
    @Override
    public Component newComponent() {
        return new WildFlyWebComponent(this.service);
    }

    private final UndertowService service;
}
