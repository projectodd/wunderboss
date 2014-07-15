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

import io.undertow.servlet.api.SessionManagerFactory;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.service.*;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.session.*;

public class WildFlyServiceActivator implements ServiceActivator {
    @Override
    public void activate(ServiceActivatorContext serviceActivatorContext) throws ServiceRegistryException {
        Module module = Module.forClass(WildFlyServiceActivator.class);
        String deploymentName = module.getIdentifier().getName();
        if (deploymentName.startsWith("deployment.")) {
            deploymentName = deploymentName.replace("deployment.", "");
        }

        WildFlyService service = new WildFlyService(deploymentName, serviceActivatorContext.getServiceRegistry());
        ServiceBuilder builder = serviceActivatorContext.getServiceTarget()
                .addService(WildFlyService.serviceName(deploymentName), service)
                .addDependency(UndertowService.UNDERTOW, UndertowService.class, service.getUndertowInjector())
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL, WildFlyService.JMS_MANAGER_SERVICE_NAME)
                .addDependency(WildFlyService.WEB_CACHE_MANAGER)
                .addDependency(installSessionIdentifierCodec(serviceActivatorContext.getServiceTarget(), WildFlyService.serviceName(deploymentName)), SessionIdentifierCodec.class, service.getSessionIdentifierCodecInjector())
                .addDependency(ServiceBuilder.DependencyType.OPTIONAL,
                        ChannelFactoryService.getServiceName(null),
                        ChannelFactory.class,
                        service.getChannelFactoryInjector())
                .setInitialMode(ServiceController.Mode.ACTIVE);

        ServiceName sessionManagerFactoryServiceName = installSessionManagerFactory(serviceActivatorContext.getServiceTarget(), WildFlyService.serviceName(deploymentName), module);
        if (sessionManagerFactoryServiceName != null) {
            builder.addDependency(sessionManagerFactoryServiceName, SessionManagerFactory.class, service.getSessionManagerFactoryInjector());
        }

        builder.install();
    }

    private static ServiceName installSessionManagerFactory(ServiceTarget target, ServiceName deploymentServiceName, Module module) {
        DistributableSessionManagerFactoryBuilder sessionManagerFactoryBuilder = new DistributableSessionManagerFactoryBuilderValue().getValue();
        if (sessionManagerFactoryBuilder != null) {
            ServiceName name = deploymentServiceName.append("session");
            JBossWebMetaData metaData = new JBossWebMetaData();
            sessionManagerFactoryBuilder.build(target, name, deploymentServiceName, module, metaData).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
            return name;
        }
        return null;
    }

    private static ServiceName installSessionIdentifierCodec(ServiceTarget target, ServiceName deploymentServiceName) {
        ServiceName codecName = deploymentServiceName.append("codec");
        DistributableSessionIdentifierCodecBuilder sessionIdentifierCodecBuilder = new DistributableSessionIdentifierCodecBuilderValue().getValue();
        if (sessionIdentifierCodecBuilder != null) {
            sessionIdentifierCodecBuilder.build(target, codecName, deploymentServiceName).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
            return codecName;
        }
        SimpleSessionIdentifierCodecService.build(target, codecName).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        return codecName;
    }

}
