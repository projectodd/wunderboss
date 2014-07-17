/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss.wildfly;

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
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

        WildFlyService service = new WildFlyService(deploymentName, serviceActivatorContext.getServiceRegistry());
        serviceActivatorContext.getServiceTarget()
                .addService(WildFlyService.serviceName(deploymentName), service)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
