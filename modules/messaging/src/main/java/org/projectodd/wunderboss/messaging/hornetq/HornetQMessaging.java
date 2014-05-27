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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HornetQMessaging implements Messaging {

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
            if (this.defaultConnection != null) {
                this.defaultConnection.close();
                this.defaultConnection = null;
            }
            this.jmsServerManager.stop();
            this.jmsServerManager = null;
            this.started = false;
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

    @Override
    public Connection createConnection(Map<CreateConnectionOption, Object> options) throws Exception {
        final Options<CreateConnectionOption> opts = new Options<>(options);
        ConnectionFactory cf;
        if (opts.has(CreateConnectionOption.HOST)) {
            //TODO: possibly cache the remote cf's?
            TransportConfiguration config =
                    new TransportConfiguration("org.hornetq.core.remoting.impl.netty.NettyConnectorFactory",
                                               new HashMap() {{
                                                   put("host", opts.getString(CreateConnectionOption.HOST));
                                                   put("port", opts.getInt(CreateConnectionOption.PORT, 5445));
                                               }});
            cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, config);
        }  else {
            start();
            if (opts.getBoolean(CreateConnectionOption.XA,
                            (Boolean)CreateConnectionOption.XA.defaultValue)) {
            cf = (ConnectionFactory)lookupJNDI("java:/JmsXA");
            } else {
                cf = (ConnectionFactory)lookupJNDI("java:/ConnectionFactory");
            }
        }

        JMSContext context = cf.createContext();

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

            String jndiName = "queue:" + name;
            queue = lookupQueue(jndiName);
            String selector = opts.getString(CreateQueueOption.SELECTOR, "");

            if (queue == null) {
                createQueue(name, jndiName, selector,
                            opts.getBoolean(CreateQueueOption.DURABLE,
                                        (Boolean) CreateQueueOption.DURABLE.defaultValue));
                queue = lookupQueue(name);
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
            String jndiName = "topic:" + name;
            topic = lookupTopic(jndiName);

            if (topic == null) {
                createTopic(name, jndiName);
                topic = lookupTopic(name);
            }
        }

        return new HornetQTopic(topic, this);
    }

    protected void createTopic(String name, String jndiName) throws Exception {
        this.jmsServerManager.createTopic(false, name, jndiName);
    }

    protected void createQueue(String name, String jndiName, String selector, boolean durable) throws Exception {
        this.jmsServerManager.createQueue(false, name, selector, durable, jndiName);
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
    private Connection defaultConnection;
    protected boolean started = false;
    protected JMSServerManager jmsServerManager;
}
