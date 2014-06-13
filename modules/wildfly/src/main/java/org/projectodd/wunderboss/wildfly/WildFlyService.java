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
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.projectodd.wunderboss.ApplicationRunner;
import org.projectodd.wunderboss.WunderBoss;
import org.wildfly.extension.undertow.UndertowService;

import java.io.IOException;

public class WildFlyService implements Service<WildFlyService> {

    public static ServiceName parentServiceName(String deploymentName) {
        return ServiceName.JBOSS.append("deployment").append("unit").append(deploymentName);
    }

    public static ServiceName serviceName(String deploymentName) {
        return parentServiceName(deploymentName).append("wunderboss");
    }

    public WildFlyService(String deploymentName, ServiceRegistry registry) {
        this.deploymentName = deploymentName;
        this.registry = registry;
    }

    @Override
    public void start(StartContext context) throws StartException {
        WunderBoss.putOption("deployment-name", this.deploymentName);
        WunderBoss.putOption("service-registry", this.registry);
        WunderBoss.putOption("wildfly-service", this);
        WunderBoss.putOption("default-context-path", getDefaultContextPath());

        try {
            WunderBoss.registerComponentProvider(new WildflyWebProvider(undertowInjector.getValue()));
        } catch (LinkageError ignored) {
            // Ignore - perhaps the user isn't using our web
        }
        try {
            WunderBoss.registerComponentProvider(new WildFlyMessagingProvider());
        } catch (LinkageError ignored) {
            // Ignore - perhaps the user isn't using our messaging
        }
        WunderBoss.registerComponentProvider(new SingletonContextProvider());
        WunderBoss.registerComponentProvider(new ChannelProvider());

        applicationRunner = new ApplicationRunner(deploymentName) {
            @Override
            protected void updateClassPath() throws Exception {
                super.updateClassPath();
                ModuleUtils.addToModuleClasspath(Module.forClass(WildFlyService.class), classPathAdditions);
            }
            @Override
            protected String jarPath() {
                ServiceName contentServiceName = parentServiceName(deploymentName).append("contents");
                VirtualFile contentFile = (VirtualFile) registry.getRequiredService(contentServiceName).getValue();
                try {
                    return VFSUtils.getPhysicalURL(contentFile).getPath();
                } catch (IOException e) {
                    log.errorf("Unable to locate deployment jar", e);
                    return super.jarPath();
                }
            }
        };

        try {
            applicationRunner.start(null);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        log.debug("Stopping WunderBoss application");
        try {
            WunderBoss.shutdownAndReset();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (applicationRunner != null) {
                applicationRunner.stop();
                applicationRunner = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public WildFlyService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private String getDefaultContextPath() {
        String path = deploymentName.replace(".jar", "");
        if (path.equals("ROOT")) {
            path = "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    public Injector<UndertowService> getUndertowInjector() {
        return undertowInjector;
    }

    public Injector<ChannelFactory> getChannelFactoryInjector() {
        return channelFactoryInjector;
    }

    public ChannelFactory channelFactory() {
        return this.channelFactoryInjector.getValue();
    }

    private final String deploymentName;
    private final ServiceRegistry registry;
    private InjectedValue<UndertowService> undertowInjector = new InjectedValue<>();
    private InjectedValue<ChannelFactory> channelFactoryInjector = new InjectedValue<>();
    private ApplicationRunner applicationRunner;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.wildfly");

    static final ServiceName JMS_MANAGER_SERVICE_NAME = ServiceName.JBOSS.append("messaging", "default", "jms", "manager");
}
