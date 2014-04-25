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
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.registry.MapBindingRegistry;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.impl.HornetQServerImpl;
import org.hornetq.jms.client.HornetQQueue;
import org.hornetq.jms.client.HornetQTopic;
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

public class HornetQMessaging implements Messaging {

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
    public Connection createConnection(Map<CreateConnectionOption, Object> options) throws JMSException {
        Options<CreateConnectionOption> opts = new Options<>(options);
        ConnectionFactory cf;
        if (opts.getBoolean(CreateConnectionOption.XA, isXaDefault())) {
            cf = (ConnectionFactory)lookupJNDI("java:/JmsXA");
        } else {
            cf = (ConnectionFactory)lookupJNDI("java:/ConnectionFactory");
        }

        javax.jms.Connection connection = cf.createConnection();
        connection.start();

        return new HornetQConnection(this, connection);
    }

    @Override
    public synchronized HornetQEndpoint findOrCreateEndpoint(String name,
                                                   Map<CreateEndpointOption, Object> options) throws Exception {
        Options<CreateEndpointOption> opts = new Options<>(options);
        boolean topic = opts.getBoolean(CreateEndpointOption.BROADCAST, false);
        String jndiName = (topic ? "topic:" : "queue:") + name;
        Destination dest = topic ? lookupTopic(name) : lookupQueue(name);
        boolean durable = false;
        if (dest == null) {
            if (topic) {
                createTopic(name, jndiName);
                dest = lookupTopic(name);
            } else {
                durable = opts.getBoolean(CreateEndpointOption.DURABLE,
                                          (Boolean)CreateEndpointOption.DURABLE.defaultValue);
                String selector = opts.getString(CreateEndpointOption.SELECTOR, "");
                createQueue(name, jndiName, selector, durable);
                dest = lookupQueue(name);
            }
        }

        return new HornetQEndpoint(dest, this.jmsServerManager, durable);
    }

    protected void createTopic(String name, String jndiName) throws Exception {
        this.jmsServerManager.createTopic(false, name, jndiName);
    }

    protected void createQueue(String name, String jndiName, String selector, boolean durable) throws Exception {
        this.jmsServerManager.createQueue(false, name, selector, durable, jndiName);
    }

    @Override
    public boolean isXaDefault() {
        return this.xa;
    }

    protected HornetQTopic lookupTopic(String name) {
        String[] jndiNames = jmsServerManager.getJNDIOnTopic(name);
        for (String jndiName : jndiNames) {
            Object jndiObject = lookupJNDI(jndiName);
            if (jndiObject != null) {
                return (HornetQTopic) jndiObject;
            }
        }
        return null;
    }

    protected HornetQQueue lookupQueue(String name) {
        String[] jndiNames = jmsServerManager.getJNDIOnQueue(name);
        for (String jndiName : jndiNames) {
            Object jndiObject = lookupJNDI(jndiName);
            if (jndiObject != null) {
                return (HornetQQueue) jndiObject;
            }
        }
        return null;
    }

    protected Object lookupJNDI(String jndiName) {
        return jmsServerManager.getRegistry().lookup(jndiName);
    }

    private final String name;
    private final Options<CreateOption> options;
    private final boolean xa;
    protected boolean started = false;
    protected JMSServerManager jmsServerManager;
    private final Map<String, MessageHandlerGroup> listenerGroups = new ConcurrentHashMap<>();


}
