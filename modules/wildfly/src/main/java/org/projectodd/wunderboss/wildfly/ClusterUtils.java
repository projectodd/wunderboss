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

    private static final String WF8_CHANNEL_FACTORY_CLASS_NAME = "org.jboss.as.clustering.jgroups.ChannelFactory";
    private static final String WF9_CHANNEL_FACTORY_CLASS_NAME = "org.wildfly.clustering.jgroups.ChannelFactory";

    public static JChannel lockableChannel(final String id) throws Exception {
        Class channelInterface;
        try {
            channelInterface = Class.forName(WF8_CHANNEL_FACTORY_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            try {
                channelInterface = Class.forName(WF9_CHANNEL_FACTORY_CLASS_NAME);
            } catch (ClassNotFoundException e2) {
                throw new RuntimeException("Failed to find the ChannelFactory interface", e2);
            }
        }
 
        if (channelInterface == null) {
            return null;
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

    private static final ServiceName WF8_JGROUPS_STACK_NAME = ServiceName.parse("jboss.jgroups.stack");
    private static final ServiceName WF9_JGROUPS_STACK_NAME = ServiceName.parse("jboss.jgroups.factory.default-stack");

    public static Object channelFactory() {
        ServiceRegistry registry = (ServiceRegistry)WunderBoss.options().get("service-registry");
        if (registry != null) {
            ServiceController<?> serviceController = registry.getService(WF8_JGROUPS_STACK_NAME);
            if (serviceController == null) {
                serviceController = registry.getService(WF9_JGROUPS_STACK_NAME);
            }
            return serviceController == null ? null : serviceController.getValue();
        }

        return null;
    }
}
