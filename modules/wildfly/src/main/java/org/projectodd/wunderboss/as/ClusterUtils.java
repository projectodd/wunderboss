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

package org.projectodd.wunderboss.as;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceName;
import org.jgroups.JChannel;
import org.jgroups.protocols.CENTRAL_LOCK;
import org.projectodd.wunderboss.WunderBoss;
import java.lang.reflect.Method;
 
 
public class ClusterUtils {
 
    public static boolean inCluster() {
        return channelFactory() != null;
    }

    private static final String[] CHANNEL_FACTORY_CLASS_NAMES =
            { "org.jboss.as.clustering.jgroups.ChannelFactory",  // WF8
              "org.wildfly.clustering.jgroups.ChannelFactory" }; // WF9, WF10

    public static JChannel lockableChannel(final String id) throws Exception {
        Class channelInterface = null;
        Exception failure = null;
        for (String each : CHANNEL_FACTORY_CLASS_NAMES) {
            try {
                channelInterface = Class.forName(each);
                if (channelInterface != null) {
                    break;
                }
            } catch (ClassNotFoundException e) {
                failure = e;
            }
        }
 
        if (channelInterface == null) {
            throw new RuntimeException("Failed to find the ChannelFactory interface", failure);
        }

        final Method createChannel = channelInterface.getDeclaredMethod("createChannel", String.class);

        final JChannel chan = (JChannel)createChannel.invoke(channelFactory(), id);
 
        //TODO: check the stack and see if it already contains a lock proto
        // and we should doc that as the preferred way, since you can configure the number of backups?
        final CENTRAL_LOCK l = new CENTRAL_LOCK();
        l.setNumberOfBackups(1);
 
        chan.getProtocolStack().addProtocol(l);
 
        l.init();
 
        return chan;
    }

    private static final ServiceName[] JGROUPS_FACTORY_NAMES =
            { ServiceName.parse("jboss.jgroups.stack"),                 // WF8
              ServiceName.parse("jboss.jgroups.factory.default-stack"), // WF9
              ServiceName.parse("jboss.jgroups.factory.default") };     // WF10

    public static Object channelFactory() {
        ServiceRegistry registry = (ServiceRegistry)WunderBoss.options().get("service-registry");
        ServiceController<?> serviceController = null;
        if (registry != null) {
            for(ServiceName each : JGROUPS_FACTORY_NAMES) {
                serviceController = registry.getService(each);
                if (serviceController != null) {
                    break;
                }
            }
        }

        return serviceController == null ? null : serviceController.getValue();
    }
}
