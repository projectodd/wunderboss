package org.projectodd.wunderboss.messaging.hornetq;

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
import org.projectodd.wunderboss.messaging.Connection;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.Messaging;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HornetQMessaging implements Messaging<JMSServerManager, Destination, javax.jms.Connection> {

    public HornetQMessaging(String name, Options<CreateOption> options) {
        this.name = name;
        this.options = options;
        this.xa = options.getBoolean(CreateOption.XA, false);
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
            // My gut currently says "no", but in the container, we may be fighting this
            config.setPersistenceEnabled(false);

            //TODO: mbean server
            this.jmsServerManager =
                    new JMSServerManagerImpl(new HornetQServerImpl(config, new HornetQSecurityManagerImpl()),
                                             new MapBindingRegistry());
            this.jmsServerManager.start();

            List<String> connectorNames = new ArrayList<>();
            connectorNames.add("in-vm");

            this.jmsServerManager.createConnectionFactory("cf", false,
                                                          JMSFactoryType.CF,
                                                          connectorNames, "cf");
            this.jmsServerManager.createConnectionFactory("xa-cf", false,
                                                          JMSFactoryType.XA_CF,
                                                          connectorNames, "xa-cf");

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
    public Connection createConnection(Map<CreateConnectionOption, Object> options) throws JMSException {
        Options<CreateConnectionOption> opts = new Options<>(options);
        ConnectionFactory cf;
        if (opts.getBoolean(CreateConnectionOption.XA, isXaDefault())) {
            cf = (ConnectionFactory)lookup("xa-cf");
        } else {
            cf = (ConnectionFactory)lookup("cf");
        }

        javax.jms.Connection connection = cf.createConnection();
        connection.start();

        return new HornetQConnection(this, connection);
    }

    @Override
    public synchronized Endpoint findOrCreateEndpoint(String name,
                                                   Map<CreateEndpointOption, Object> options) throws Exception {
        Options<CreateEndpointOption> opts = new Options<>(options);
        boolean topic = opts.getBoolean(CreateEndpointOption.BROADCAST, false);
        String jndiName = (topic ? "topic:" : "queue:") + name;
        Destination dest = (Destination)lookup(jndiName);
        boolean durable = false;
        if (dest == null) {
            if (topic) {
                this.jmsServerManager.createTopic(true, name, jndiName);
            } else {
                durable = opts.getBoolean(CreateEndpointOption.DURABLE,
                                          (Boolean)CreateEndpointOption.DURABLE.defaultValue);
                this.jmsServerManager.createQueue(true, name,
                                                  opts.getString(CreateEndpointOption.SELECTOR, ""),
                                                  durable,
                                                  jndiName);
            }
            dest = (Destination)lookup(jndiName);
        }

        return new HornetQEndpoint(dest, this.jmsServerManager, durable);
    }

    @Override
    public boolean isXaDefault() {
        return this.xa;
    }

    protected Object lookup(String name) {
        return this.jmsServerManager.getRegistry().lookup(name);
    }

    private final String name;
    private final Options<CreateOption> options;
    private final boolean xa;
    private boolean started = false;
    private JMSServerManager jmsServerManager;
    private final Map<String, MessageHandlerGroup> listenerGroups = new ConcurrentHashMap<>();


}
