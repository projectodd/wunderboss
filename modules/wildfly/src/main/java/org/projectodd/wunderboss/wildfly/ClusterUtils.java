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
