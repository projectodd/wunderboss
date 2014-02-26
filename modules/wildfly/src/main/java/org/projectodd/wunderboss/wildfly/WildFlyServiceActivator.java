package org.projectodd.wunderboss.wildfly;

import org.jboss.as.server.deployment.Services;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;

public class WildFlyServiceActivator implements ServiceActivator {
    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {
        Module module = Module.forClass(WildFlyServiceActivator.class);
        String deploymentName = module.getIdentifier().getName();
        if (deploymentName.startsWith("deployment.")) {
            deploymentName = deploymentName.replace("deployment.", "");
        }

        ServiceName parentServiceName = ServiceName.JBOSS.append("deployment").append("unit").append(deploymentName);

        ServiceName serviceName = ServiceName.of(parentServiceName, "wunderboss");
        Service service = new WildFlyService(deploymentName, serviceActivatorContext.getServiceRegistry());
        serviceActivatorContext.getServiceTarget().addService(serviceName, service)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
