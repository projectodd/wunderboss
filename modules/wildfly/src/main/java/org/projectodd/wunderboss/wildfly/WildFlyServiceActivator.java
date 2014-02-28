package org.projectodd.wunderboss.wildfly;

import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.wildfly.extension.undertow.UndertowService;

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
        WildFlyService service = new WildFlyService(deploymentName);
        serviceActivatorContext.getServiceTarget().addService(serviceName, service)
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.getUndertowInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
