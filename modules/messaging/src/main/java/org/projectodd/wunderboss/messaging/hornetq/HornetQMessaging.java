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

package org.projectodd.wunderboss.messaging.hornetq;

import org.hornetq.api.core.HornetQNotConnectedException;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.registry.MapBindingRegistry;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.hornetq.spi.core.security.HornetQSecurityManagerImpl;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Connection;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Queue;
import org.projectodd.wunderboss.messaging.Topic;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HornetQMessaging implements Messaging {

    public static final String REMOTE_TYPE_WILDFLY = "hornetq_wildfly";
    public static final String REMOTE_TYPE_STANDALONE = "hornetq_standalone";

    public HornetQMessaging(String name, Options<CreateOption> options) {
        this.name = name;
        this.options = options;
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

            config.setPersistenceEnabled(true);

            config.setBindingsDirectory("target/data/bindings");
            config.setLargeMessagesDirectory("target/data/largemessages");

            //TODO: mbean server
            this.jmsServerManager =
                    new JMSServerManagerImpl(new HornetQServerImpl(config, new HornetQSecurityManagerImpl()),
                                             new MapBindingRegistry());
            this.jmsServerManager.start();

            List<String> connectorNames = new ArrayList<>();
            connectorNames.add("in-vm");

            this.jmsServerManager.createConnectionFactory("cf", false,
                                                          JMSFactoryType.CF,
                                                          connectorNames, "java:/ConnectionFactory");
            this.jmsServerManager.createConnectionFactory("xa-cf", false,
                                                          JMSFactoryType.XA_CF,
                                                          connectorNames, "java:/JmsXA");

            this.started = true;
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        if (started) {
            closeDefaultConnection();
            this.jmsServerManager.stop();
            this.jmsServerManager = null;
            this.started = false;
        }
    }

    protected void closeDefaultConnection() throws Exception {
        if (this.defaultConnection != null) {
            this.defaultConnection.close();
            this.defaultConnection = null;
        }
    }

    public JMSServerManager jmsServerManager() {
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
    public Connection defaultConnection() throws Exception {
        if (this.defaultConnection == null) {
            this.defaultConnection = createConnection(null);
        }
        return this.defaultConnection;
    }

    private JMSContext createContext(ConnectionFactory cf, Options<CreateConnectionOption> options) {
        if (options.has(CreateConnectionOption.USERNAME)) {
            return cf.createContext(options.getString(CreateConnectionOption.USERNAME),
                                    options.getString(CreateConnectionOption.PASSWORD));
        } else {
            return cf.createContext();
        }
    }

    private ConnectionFactory createHQConnectionFactory(final Options<CreateConnectionOption> options) {
        //TODO: possibly cache the remote cf's?
        TransportConfiguration config =
                new TransportConfiguration("org.hornetq.core.remoting.impl.netty.NettyConnectorFactory",
                        new HashMap() {{
                            put("host", options.getString(CreateConnectionOption.HOST));
                            put("port", options.getInt(CreateConnectionOption.PORT));
                            put("http-upgrade-enabled",
                                    REMOTE_TYPE_WILDFLY.equals(options.getString(CreateConnectionOption.REMOTE_TYPE)));
                        }});
        HornetQConnectionFactory hornetQcf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, config);

        hornetQcf.setReconnectAttempts(options.getInt(CreateConnectionOption.RECONNECT_ATTEMPTS));
        hornetQcf.setRetryInterval(options.getLong(CreateConnectionOption.RECONNECT_RETRY_INTERVAL));
        hornetQcf.setRetryIntervalMultiplier(options.getDouble(CreateConnectionOption.RECONNECT_RETRY_INTERVAL_MULTIPLIER));
        hornetQcf.setMaxRetryInterval(options.getLong(CreateConnectionOption.RECONNECT_MAX_RETRY_INTERVAL));

        return hornetQcf;
    }

    @Override
    public Connection createConnection(Map<CreateConnectionOption, Object> options) throws Exception {
        final Options<CreateConnectionOption> opts = new Options<>(options);
        ConnectionFactory cf;
        JMSContext context;

        if (opts.has(CreateConnectionOption.HOST)) {
                context = createContext(createHQConnectionFactory(opts), opts);
        }  else {
            start();

            if (opts.getBoolean(CreateConnectionOption.XA)) {
            cf = (ConnectionFactory)lookupJNDI("java:/JmsXA");
            } else {
                cf = (ConnectionFactory)lookupJNDI("java:/ConnectionFactory");
            }
            context = createContext(cf, opts);
        }

        if (opts.has(CreateConnectionOption.CLIENT_ID)) {
            context.setClientID(opts.getString(CreateConnectionOption.CLIENT_ID));
        }

        return new HornetQConnection(context, this, opts);
    }

    @Override
    public synchronized Queue findOrCreateQueue(String name,
                                                Map<CreateQueueOption, Object> options) throws Exception {
        Options<CreateQueueOption> opts = new Options<>(options);
        javax.jms.Queue queue;
        if (opts.has(CreateQueueOption.CONNECTION)) {
            // assume it's remote, so we just need a ref to it
            queue = ((HornetQConnection)opts.get(CreateQueueOption.CONNECTION)).jmsContext().createQueue(name);
        } else {
            start();

            queue = lookupQueue(name);
            String selector = opts.getString(CreateQueueOption.SELECTOR, "");

            if (queue == null) {
                queue = createQueue(name, selector,
                                    opts.getBoolean(CreateQueueOption.DURABLE));
                this.createdDestinations.add(HornetQQueue.fullName(name));
            }
        }

        return new HornetQQueue(queue, this);
    }

    @Override
    public synchronized Topic findOrCreateTopic(String name,
                                                Map<CreateTopicOption, Object> options) throws Exception {
        Options<CreateTopicOption> opts = new Options<>(options);
        javax.jms.Topic topic;
        if (opts.has(CreateTopicOption.CONNECTION)) {
            // assume it's remote, so we just need a ref to it
            topic = ((HornetQConnection)opts.get(CreateTopicOption.CONNECTION)).jmsContext().createTopic(name);
        } else {
            start();
            topic = lookupTopic(name);

            if (topic == null) {
                topic = createTopic(name);
                this.createdDestinations.add(HornetQTopic.jmsName(name));
            }
        }

        return new HornetQTopic(topic, this);
    }

    protected javax.jms.Topic createTopic(String name) throws Exception {
        this.jmsServerManager.createTopic(false, name, "java:/jms/topic/" + name);

        return lookupTopic(name);
    }

    protected javax.jms.Queue createQueue(String name, String selector, boolean durable) throws Exception {
        this.jmsServerManager.createQueue(false, name, selector, durable, "java:/jms/queue/" + name);

        return lookupQueue(name);
    }

    protected boolean destroyDestination(HornetQDestination dest) throws Exception {
        String fullName = dest.jmsName();
        if (this.closeablesForDestination.containsKey(fullName)) {
            for(AutoCloseable each : this.closeablesForDestination.get(fullName)) {
                each.close();
            }
            this.closeablesForDestination.remove(fullName);
        }

        if (this.createdDestinations.contains(fullName)) {
            if (dest instanceof HornetQQueue) {
                this.jmsServerManager().destroyQueue(dest.name(), true);
            } else {
                this.jmsServerManager().destroyTopic(dest.name(), true);
            }
            this.createdDestinations.remove(fullName);

            return true;
        }

        return false;
    }

    protected void addCloseableForDestination(HornetQDestination dest, AutoCloseable c) {
        String fullName = dest.jmsName();
        List<AutoCloseable> closeables = this.closeablesForDestination.get(fullName);
        if (closeables == null) {
            closeables = new ArrayList<>();
            this.closeablesForDestination.put(fullName, closeables);
        }

        closeables.add(c);
    }

    protected javax.jms.Topic lookupTopic(String name) {
        String[] jndiNames = jmsServerManager.getJNDIOnTopic(name);
        for (String jndiName : jndiNames) {
            Object jndiObject = lookupJNDI(jndiName);
            if (jndiObject != null) {
                return (javax.jms.Topic)jndiObject;
            }
        }
        return null;
    }

    protected javax.jms.Queue lookupQueue(String name) {
        String[] jndiNames = jmsServerManager.getJNDIOnQueue(name);
        for (String jndiName : jndiNames) {
            Object jndiObject = lookupJNDI(jndiName);
            if (jndiObject != null) {
                return (javax.jms.Queue)jndiObject;
            }
        }
        return null;
    }

    protected Object lookupJNDI(String jndiName) {
        return jmsServerManager.getRegistry().lookup(jndiName);
    }

    private final String name;
    private final Options<CreateOption> options;
    private final Set<String> createdDestinations = new HashSet<>();
    private final Map<String, List<AutoCloseable>> closeablesForDestination = new HashMap<>();
    private Connection defaultConnection;
    protected boolean started = false;
    protected JMSServerManager jmsServerManager;

}
