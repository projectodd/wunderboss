package org.projectodd.wunderboss.wildfly;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.web.Web;
import org.wildfly.extension.undertow.UndertowService;

public class WildflyWebProvider implements ComponentProvider<Web> {

    public WildflyWebProvider(UndertowService service) {
        this.service = service;

    }
    @Override
    public Web create(String name, Options ignored) {
        return new WildFlyWeb(name, this.service);
    }

    private final UndertowService service;
}
