/*
 * Copyright 2015 Red Hat, Inc, and individual contributors.
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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.projectodd.wunderboss.ec.AlwaysMasterClusterParticipant;
import org.projectodd.wunderboss.ec.ClusterParticipant;

public class DaemonContextProvider extends org.projectodd.wunderboss.ec.DaemonContextProvider {
    public DaemonContextProvider(final ServiceRegistry registry, final ServiceTarget target) {
        this.registry = registry;
        this.target = target;
    }

    @Override
    protected ClusterParticipant clusterParticipant(final String name, final boolean singleton) {
        ClusterParticipant participant;
        if (singleton
                && ClusterUtils.inCluster()) {
            participant = new SingletonClusterParticipant();
            ClusterUtils.installSingleton(this.registry, this.target, (Service)participant,
                                          "daemon-" + name);
        } else {
            participant = AlwaysMasterClusterParticipant.INSTANCE;
        }

        return participant;
    }

    private final ServiceRegistry registry;
    private final ServiceTarget target;
}
