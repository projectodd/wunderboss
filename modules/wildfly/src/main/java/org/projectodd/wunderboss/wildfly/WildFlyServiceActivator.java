package org.projectodd.wunderboss.wildfly;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;

public class WildFlyServiceActivator implements ServiceActivator {
    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {
        ServiceName serviceName = ServiceName.parse("wunderboss.foo");
        Service service = new WildFlyService(serviceActivatorContext.getServiceRegistry());
        serviceActivatorContext.getServiceTarget().addService(serviceName, service)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
