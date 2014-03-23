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

import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryService;
import org.jboss.msc.service.ServiceRegistry;
import org.jgroups.JChannel;
import org.jgroups.protocols.CENTRAL_LOCK;
import org.projectodd.wunderboss.WunderBoss;


public class ClusterUtils {

    public static boolean inCluster() {
        ServiceRegistry registry = (ServiceRegistry)WunderBoss.options().get("service-registry");

        return (registry != null &&
                registry.getService(ChannelFactoryService.getServiceName(null)) != null);
    }

    public static JChannel lockableChannel(String id) throws Exception {
        WildFlyService wfService = (WildFlyService)WunderBoss.options().get("wildfly-service");
        JChannel chan = (JChannel)wfService.channelFactory().createChannel(id);

        //TODO: check the stack and see if it already contains a lock proto
        // and we should doc that as the preferred way, since you can configure the number of backups?
        CENTRAL_LOCK l = new CENTRAL_LOCK();
        l.setNumberOfBackups(1);

        chan.getProtocolStack().addProtocol(l);

        l.init();

        return chan;
    }
}
