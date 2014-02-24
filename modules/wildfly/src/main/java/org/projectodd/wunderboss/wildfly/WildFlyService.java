package org.projectodd.wunderboss.wildfly;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.projectodd.wunderboss.Application;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.ruby.rack.RackComponent;

public class WildFlyService implements Service<WildFlyService> {

    public WildFlyService(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;

        Options options = new Options();
        options.put("root", "/home/bbrowning/tmp/blah2");
        container = new WunderBoss(options);
        container.registerLanguage("ruby", new WildFlyRubyLanguage());
        container.registerComponent("web", new WildFlyWebComponent(serviceRegistry));
        container.registerComponent("rack", new RackComponent());
    }

    @Override
    public void start(StartContext context) throws StartException {
        System.err.println("!!! Starting Ruby application");
        application = container.newApplication("ruby");
        application.start("rack");
    }

    @Override
    public void stop(StopContext context) {
        if (application != null) {
            System.err.println("!!! Stopping Ruby application");
            application.stop();
        }
        container.stop();
    }

    @Override
    public WildFlyService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private WunderBoss container;
    private Application application;
    private ServiceRegistry serviceRegistry;
}
