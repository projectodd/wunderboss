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

package org.projectodd.wunderboss.messaging.hornetq;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.server.JMSServerManager;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.messaging.jms.DestinationUtil;
import org.projectodd.wunderboss.messaging.jms.JMSDestination;
import org.projectodd.wunderboss.messaging.jms.JMSMessagingSkeleton;
import org.slf4j.Logger;

import javax.jms.ConnectionFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HQMessaging extends JMSMessagingSkeleton {

    public static final String REMOTE_TYPE_WILDFLY = "hornetq_wildfly";
    public static final String REMOTE_TYPE_STANDALONE = "hornetq_standalone";

    public HQMessaging(String name, Options<CreateOption> options) {
        this.name = name;
        this.options = options;
    }

    @Override
    public synchronized void start() throws Exception {
        if (!started) {
            this.server = new EmbeddedServer();

            ClassLoader cl = this.getClass().getClassLoader();
            if (cl.getResource("hornetq-configuration.xml") == null) {
                this.server.setConfigResourcePath("default-hornetq-configuration.xml");
            }

            if (cl.getResource("hornetq-jms.xml") == null) {
                this.server.setJmsConfigResourcePath("default-hornetq-jms.xml");
            }

            this.server.start();

            this.started = true;
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        if (started) {
            closeCloseables();
            this.server.stop();
            this.server = null;
            this.started = false;
        }
    }

    @Override
    public boolean isRunning() {
        return started;
    }

    public JMSServerManager jmsServerManager() {
        if (this.started) {
            return this.server.serverManager();
        }

        return null;
    }

    @Override
    public String name() {
        return this.name;
    }

    protected ConnectionFactory createRemoteConnectionFactory(final Options<CreateContextOption> options) {
        //TODO: possibly cache the remote cf's?
        Map<String, Object> transportOpts = new HashMap<>();
        transportOpts.put("host", options.getString(CreateContextOption.HOST));
        transportOpts.put("port", options.getInt(CreateContextOption.PORT));
        if (REMOTE_TYPE_WILDFLY.equals(options.getString(CreateContextOption.REMOTE_TYPE))) {
            transportOpts.put("http-upgrade-enabled", true);
        }

        TransportConfiguration config =
                new TransportConfiguration("org.hornetq.core.remoting.impl.netty.NettyConnectorFactory",
                                           transportOpts);
        HornetQConnectionFactory hornetQcf = HornetQJMSClient
                .createConnectionFactoryWithoutHA(options.has(CreateContextOption.XA) ?
                                                          JMSFactoryType.XA_CF :
                                                          JMSFactoryType.CF,
                                                  config);

        hornetQcf.setReconnectAttempts(options.getInt(CreateContextOption.RECONNECT_ATTEMPTS));
        hornetQcf.setRetryInterval(options.getLong(CreateContextOption.RECONNECT_RETRY_INTERVAL));
        hornetQcf.setRetryIntervalMultiplier(options.getDouble(CreateContextOption.RECONNECT_RETRY_INTERVAL_MULTIPLIER));
        hornetQcf.setMaxRetryInterval(options.getLong(CreateContextOption.RECONNECT_MAX_RETRY_INTERVAL));

        return hornetQcf;
    }

    protected javax.jms.Topic createTopic(String name) throws Exception {
        this.server
                .serverManager()
                .createTopic(false, name, DestinationUtil.jndiName(name, JMSDestination.Type.TOPIC));

        return lookupTopic(name);
    }

    protected javax.jms.Queue createQueue(String name, String selector, boolean durable) throws Exception {
        this.server
                .serverManager()
                .createQueue(false, name, selector, durable, DestinationUtil.jndiName(name, JMSDestination.Type.QUEUE));

        return lookupQueue(name);
    }

    protected javax.jms.Topic lookupTopic(String name) {
        List<String> jndiNames = new ArrayList<>();

        if (this.server != null) {
            jndiNames.addAll(Arrays.asList(this.server.serverManager().getJNDIOnTopic(name)));
        }
        jndiNames.add(name);
        jndiNames.add(DestinationUtil.jmsName(name, JMSDestination.Type.TOPIC));
        jndiNames.add(DestinationUtil.jndiName(name, JMSDestination.Type.TOPIC));

        return (javax.jms.Topic)lookupJNDI(jndiNames);
    }

    protected javax.jms.Queue lookupQueue(String name) {
        List<String> jndiNames = new ArrayList<>();

        if (this.server != null) {
            jndiNames.addAll(Arrays.asList(this.server.serverManager().getJNDIOnQueue(name)));
        }
        jndiNames.add(name);
        jndiNames.add(DestinationUtil.jmsName(name, JMSDestination.Type.QUEUE));
        jndiNames.add(DestinationUtil.jndiName(name, JMSDestination.Type.QUEUE));

        return (javax.jms.Queue)lookupJNDI(jndiNames);
    }

    protected void destroyQueue(String name) throws Exception {
        this.jmsServerManager().destroyQueue(name, true);
    }

    protected void destroyTopic(String name) throws Exception {
        this.jmsServerManager().destroyTopic(name, true);
    }

    protected Object lookupJNDI(String jndiName) {
        return server.getRegistry().lookup(jndiName);
    }

    private final String name;
    private final Options<CreateOption> options;
    protected boolean started = false;
    protected EmbeddedServer server;

    private final static Logger log = WunderBoss.logger("org.projectodd.wunderboss.messaging.hornetq");
}
