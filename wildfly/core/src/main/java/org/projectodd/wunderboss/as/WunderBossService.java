/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.as;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.projectodd.wunderboss.WunderBoss;

import javax.naming.Context;

public class WunderBossService implements Service<WunderBossService> {
    public static final String KEY = "wunderboss-msc-service";

    public static ServiceName parentServiceName(String deploymentName) {
        return ServiceName.JBOSS.append("deployment").append("unit").append(deploymentName);
    }

    public static ServiceName serviceName(String deploymentName) {
        return parentServiceName(deploymentName).append("wunderboss");
    }

    public WunderBossService(String deploymentName, ServiceRegistry registry, ServiceTarget serviceTarget, Context namingContext) {
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
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public WunderBossService getValue() throws IllegalStateException, IllegalArgumentException {
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

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.as");
}
