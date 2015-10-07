/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.DelegatingServiceContainer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.as.wildfly.RandomSingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

import java.lang.reflect.InvocationTargetException;


public class ClusterUtils {
    private static final ServiceName[] SINGLETON_FACTORY_NAMES =
            { ServiceName.parse("jboss.clustering.singleton.builder.server.default"), // WF8,9
              ServiceName.parse("jboss.clustering.singleton.server.default") };       // WF10

    private static final ServiceName SINGLETON_NAME = ServiceName.of("wunderboss", "singleton");

    private static final ServiceName[] JGROUPS_FACTORY_NAMES =
            { ServiceName.parse("jboss.jgroups.stack"),                 // WF8, EAP
              ServiceName.parse("jboss.jgroups.factory.default-stack"), // WF9
              ServiceName.parse("jboss.jgroups.factory.default") };     // WF10

    public static boolean inCluster() {
        if (inCluster == null) {
            //look for a jgroups stack to see if we are clustered
            ServiceRegistry registry = (ServiceRegistry) WunderBoss.options().get("service-registry");
            if (registry != null) {
                for (ServiceName each : JGROUPS_FACTORY_NAMES) {
                    if (registry.getService(each) != null) {
                        inCluster = true;
                        break;
                    }
                }
            }

            if (inCluster == null) {
                inCluster = false;
            }
        }

        return inCluster;
    }

    //TODO: expose election policy, quorum?
    public static void installSingleton(final ServiceRegistry registry,
                                        final ServiceTarget target,
                                        final Service service,
                                        final String name) {
        final String deploymentName = (String)WunderBoss.options().get("deployment-name");
        final ServiceName serviceName = SINGLETON_NAME.append(deploymentName).append(name);
        SingletonServiceBuilderFactory factory = null;
        for(ServiceName each : SINGLETON_FACTORY_NAMES) {
            ServiceController factoryService = registry.getService(each);
            if (factoryService != null) {
                factory = (SingletonServiceBuilderFactory)factoryService.getValue();
            }
        }

        if (factory == null) {
            throw new RuntimeException("Failed to locate singleton builder");
        }

        final InjectedValue<ServerEnvironment> env = new InjectedValue<>();
        factory.createSingletonServiceBuilder(serviceName, service)
                .electionPolicy((SingletonElectionPolicy)electionPolicy())
                .requireQuorum(1)
                .build(target)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, env)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private static Object electionPolicy() {
        return new RandomSingletonElectionPolicy();
    }

    private static Boolean inCluster = null;
}
