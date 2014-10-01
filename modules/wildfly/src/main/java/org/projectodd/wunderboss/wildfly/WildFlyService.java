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

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.caching.Caching;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.singleton.SingletonContext;

import javax.naming.Context;
import javax.naming.InitialContext;

public class WildFlyService implements Service<WildFlyService> {
    public static final String KEY = "wildfly-service";

    public static ServiceName parentServiceName(String deploymentName) {
        return ServiceName.JBOSS.append("deployment").append("unit").append(deploymentName);
    }

    public static ServiceName serviceName(String deploymentName) {
        return parentServiceName(deploymentName).append("wunderboss");
    }

    public WildFlyService(String deploymentName, ServiceRegistry registry, ServiceTarget serviceTarget, Context namingContext) {
        this.deploymentName = deploymentName;
        this.serviceRegistry = registry;
        this.serviceTarget = serviceTarget;
        this.namingContext = namingContext;

        // TODO: Get rid of these options and just make them statics here
        WunderBoss.putOption("deployment-name", this.deploymentName);
        WunderBoss.putOption("service-registry", this.serviceRegistry);
        WunderBoss.putOption(KEY, this);
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            WunderBoss.registerComponentProvider(Messaging.class, new WildFlyMessagingProvider());
        } catch (LinkageError ignored) {
            // Ignore - perhaps the user isn't using our messaging
        }
        try {
            WunderBoss.registerComponentProvider(Caching.class, new WildFlyCachingProvider());
        } catch (LinkageError ignored) {
            // Ignore - perhaps the user isn't using our caching
        }
        WunderBoss.registerComponentProvider(SingletonContext.class, new SingletonContextProvider());
        WunderBoss.registerComponentProvider(ChannelWrapper.class, new ChannelProvider());
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public WildFlyService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public ServiceTarget serviceTarget() {
        return this.serviceTarget;
    }

    public ServiceRegistry serviceRegistry() {
        return this.serviceRegistry;
    }

    public Context namingContext() {
        return this.namingContext;
    }

    private final String deploymentName;
    private final ServiceRegistry serviceRegistry;
    private final ServiceTarget serviceTarget;
    private final Context namingContext;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.wildfly");

    static final ServiceName JMS_MANAGER_SERVICE_NAME = ServiceName.JBOSS.append("messaging", "default", "jms", "manager");
    static final ServiceName WEB_CACHE_MANAGER = ServiceName.JBOSS.append("infinispan", "web");
}
