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

package org.projectodd.wunderboss.as.singletons;

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
import org.projectodd.wunderboss.as.ASUtils;
import org.projectodd.wunderboss.as.ModuleUtils;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;

import java.lang.reflect.InvocationTargetException;


public class SingletonHelper {
    private static final ServiceName[] SINGLETON_FACTORY_NAMES =
            { ServiceName.parse("jboss.clustering.singleton.builder.server.default"), // WF8,9
              ServiceName.parse("jboss.clustering.singleton.server.default") };       // WF10

    private static final ServiceName SINGLETON_NAME = ServiceName.of("wunderboss", "singleton");

    //TODO: expose election policy, quorum?
    public static void installSingleton(final ServiceRegistry registry,
                                        final ServiceTarget target,
                                        final Service service,
                                        final String name) {
        final String deploymentName = (String)WunderBoss.options().get("deployment-name");
        final ServiceName serviceName = SINGLETON_NAME.append(deploymentName).append(name);
        if (ASUtils.containerIsEAP()) {
            installEAPSingleton(registry, target, service, serviceName);
        } else {
            installWildFlySingleton(registry, target, service, serviceName);
        }
    }

    private static void installEAPSingleton(final ServiceRegistry registry,
                                            final ServiceTarget target,
                                            final Service service,
                                            final ServiceName name) {
        try {
            Class clazz = SingletonHelper.class.getClassLoader()
                    .loadClass("org.jboss.as.clustering.singleton.SingletonService");
            Object singletonService = clazz.getDeclaredConstructor(Service.class, ServiceName.class)
                    .newInstance(service, name);
            ModuleUtils.lookupMethodByName(clazz, "setElectionPolicy").invoke(singletonService, electionPolicy());
            ServiceBuilder builder = (ServiceBuilder)ModuleUtils.lookupMethod(clazz, "build", ServiceTarget.class)
                    .invoke(singletonService, new DelegatingServiceContainer(target, registry));
            builder.setInitialMode(ServiceController.Mode.ACTIVE).install();

        } catch (ClassNotFoundException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException("Failed to install singleton service", e);
        }
    }



    private static void installWildFlySingleton(final ServiceRegistry registry,
                                                final ServiceTarget target,
                                                final Service service,
                                                final ServiceName name) {
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
        factory.createSingletonServiceBuilder(name, service)
                .electionPolicy((SingletonElectionPolicy)electionPolicy())
                .requireQuorum(1)
                .build(target)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, env)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    private static Object electionPolicy() {
        String className;
        if (ASUtils.containerIsEAP()) {
            className = "org.jboss.as.clustering.singleton.election.SimpleSingletonElectionPolicy";
        } else if (ASUtils.containerIsWildFly10()) {
            className = "org.wildfly.clustering.singleton.election.RandomSingletonElectionPolicy";
        } else {
            // non-deterministic election policies (in WF versions < 10) can cause multiple or no
            // singletons to start in the cluster, since each node determines the master individually
            // so we use the simple policy (which is deterministic) if we're not in WF10.
            // see https://issues.jboss.org/browse/WFLY-5108
            className = "org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy";
        }

        try {
            Class clazz = SingletonHelper.class.getClassLoader().loadClass(className);

            return clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to load election policy", e);
        }
    }

}
