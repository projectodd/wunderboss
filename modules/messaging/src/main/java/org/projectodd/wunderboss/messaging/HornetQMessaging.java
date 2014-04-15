package org.projectodd.wunderboss.messaging;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.registry.MapBindingRegistry;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.hornetq.spi.core.security.HornetQSecurityManagerImpl;
import org.projectodd.wunderboss.Options;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HornetQMessaging implements Messaging<JMSServerManager> {

    public HornetQMessaging(String name, Options<CreateOption> options) {
        this.name = name;
        //TODO: honor/use options
    }

    @Override
    public synchronized void start() throws Exception {
        if (!started) {
            Configuration config = new ConfigurationImpl();
            Set<TransportConfiguration> transports = new HashSet<>();

            transports.add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
            transports.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));

            config.setAcceptorConfigurations(transports);

            Map<String, TransportConfiguration> connectors = new HashMap<>();
            connectors.put("in-vm", new TransportConfiguration(InVMConnectorFactory.class.getName()));

            config.setConnectorConfigurations(connectors);

            config.setJournalType(JournalType.NIO);
            config.setJournalDirectory("target/data/journal");

            // TODO: security?
            config.setSecurityEnabled(false);

            //TODO: setting this to true allows persisting destination configs. Do we want this?
            // My gut currently says "no", but in the container, we maybe be fighting this
            config.setPersistenceEnabled(false);

            //TODO: mbean server
            this.jmsServerManager =
                    new JMSServerManagerImpl(new HornetQServerImpl(config, new HornetQSecurityManagerImpl()),
                                             new MapBindingRegistry());
            this.jmsServerManager.start();

            List<String> connectorNames = new ArrayList<>();
            connectorNames.add("in-vm");

            this.jmsServerManager.createConnectionFactory("cf", false, JMSFactoryType.XA_CF, connectorNames, "cf");

            this.started = true;
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        if (started) {
            this.jmsServerManager.stop();
            this.jmsServerManager = null;
            this.started = false;
        }
    }

    @Override
    public JMSServerManager implementation() {
        if (this.started) {
            return this.jmsServerManager;
        }

        return null;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Connection createConnection() throws JMSException {
        ConnectionFactory cf = (ConnectionFactory)lookup("cf");

        return cf.createConnection();
    }

    @Override
    public synchronized Queue findOrCreateQueue(String name,
                                                Map<CreateQueueOption, Object> options) throws Exception {
        Options<CreateQueueOption> opts = new Options<>(options);
        String jndiName = "queue:" + name;
        Queue queue = (Queue)lookup(jndiName);
        if (queue == null) {
            this.jmsServerManager.createQueue(true, name,
                                              opts.getString(CreateQueueOption.SELECTOR, ""),
                                              opts.getBoolean(CreateQueueOption.DURABLE, true),
                                              jndiName);
            queue = (Queue)lookup(jndiName);
        }

        return queue;
    }

    @Override
    public synchronized Topic findOrCreateTopic(String name) throws Exception {
        String jndiName = "topic:" + name;
        Topic topic = (Topic)lookup(jndiName);
        if (topic == null) {
            this.jmsServerManager.createTopic(true, name, jndiName);
            topic = (Topic)lookup(jndiName);
        }

        return topic;
    }

    /*
    TODO: in-container should be smarter, and only destroy queues this instance created
     */
    @Override
    public boolean releaseQueue(String name, boolean removeConsumers) throws Exception {
        return this.jmsServerManager.destroyQueue(name, removeConsumers);
    }

    @Override
    public boolean releaseTopic(String name, boolean removeConsumers) throws Exception {
        return this.jmsServerManager.destroyTopic(name, removeConsumers);
    }

    protected Object lookup(String name) {
        return this.jmsServerManager.getRegistry().lookup(name);
    }

    private final String name;
    private boolean started = false;
    private JMSServerManager jmsServerManager;


}
