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
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.client.HornetQXAConnectionFactory;
import org.hornetq.jms.server.JMSServerManager;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Queue;
import org.projectodd.wunderboss.messaging.Topic;
import org.slf4j.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HQMessaging implements Messaging {

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

    private HQSpecificContext createContext(ConnectionFactory cf, Options<CreateContextOption> options) {
        int mode = HQContext.modeToJMSMode((Context.Mode) options.get(Messaging.CreateContextOption.MODE));
        JMSContext jmsContext;

        if (options.has(CreateContextOption.USERNAME)) {
            jmsContext = cf.createContext(options.getString(CreateContextOption.USERNAME),
                                          options.getString(CreateContextOption.PASSWORD),
                                          mode);
        } else {
            jmsContext = cf.createContext(mode);
        }

        return new HQContext(jmsContext, this,
                                     (Context.Mode)options.get(CreateContextOption.MODE),
                                     options.has(CreateContextOption.HOST));
    }

    private HQSpecificContext createContext(String factoryName, Options<CreateContextOption> options) {
        return createContext((ConnectionFactory) lookupJNDI(factoryName), options);
    }

    private HQSpecificContext createXAContext(XAConnectionFactory cf, Options<CreateContextOption> options) {
        XAJMSContext context;

        if (HQXAContext.tm == null) {
            throw new NullPointerException("TransactionManager not found; is transactions module on the classpath?");
        }

        if (options.has(CreateContextOption.USERNAME)) {
            context = cf.createXAContext(options.getString(CreateContextOption.USERNAME),
                                      options.getString(CreateContextOption.PASSWORD));
        } else {
            context = cf.createXAContext();
        }

        return new HQXAContext(context, this,
                               (Context.Mode)options.get(CreateContextOption.MODE),
                               options.has(CreateContextOption.HOST));
    }

    private HQSpecificContext createXAContext(String factoryName, Options<CreateContextOption> options) {
        return createXAContext((XAConnectionFactory) lookupJNDI(factoryName), options);
    }

    private ConnectionFactory createHQConnectionFactory(final Options<CreateContextOption> options) {
        //TODO: possibly cache the remote cf's?
        TransportConfiguration config =
                new TransportConfiguration("org.hornetq.core.remoting.impl.netty.NettyConnectorFactory",
                        new HashMap() {{
                            put("host", options.getString(CreateContextOption.HOST));
                            put("port", options.getInt(CreateContextOption.PORT));
                            put("http-upgrade-enabled",
                                    REMOTE_TYPE_WILDFLY.equals(options.getString(CreateContextOption.REMOTE_TYPE)));
                        }});
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

    @Override
    public Context createContext(Map<CreateContextOption, Object> options) throws Exception {
        final Options<CreateContextOption> opts = new Options<>(options);
        HQSpecificContext context;

        boolean xa = opts.getBoolean(CreateContextOption.XA);
        if (opts.has(CreateContextOption.HOST)) {
            if (xa) {
                context = createXAContext((HornetQXAConnectionFactory) createHQConnectionFactory(opts), opts);
            } else {
                context = createContext(createHQConnectionFactory(opts), opts);
            }
        }  else {
            start();

            if (xa) {
                context = createXAContext("java:/JmsXA", opts);
            } else {
                context = createContext("java:/ConnectionFactory", opts);
            }
        }

        if (opts.has(CreateContextOption.CLIENT_ID)) {
            context.jmsContext().setClientID(opts.getString(CreateContextOption.CLIENT_ID));
        }


        return context;
    }

    @Override
    public synchronized Queue findOrCreateQueue(String name,
                                                Map<CreateQueueOption, Object> options) throws Exception {
        Options<CreateQueueOption> opts = new Options<>(options);
        javax.jms.Queue queue;
        HQSpecificContext givenContext = (HQSpecificContext)opts.get(CreateQueueOption.CONTEXT);
        if (givenContext != null) {
            if (!givenContext.isRemote()) {
                throw new IllegalArgumentException("Queue lookup only accepts a remote context.");
            }
            if (opts.size() > 1) {
                throw new IllegalArgumentException("Creation options provided for a remote queue.");
            }
            queue = givenContext.jmsContext().createQueue(name);
        } else {
            start();

            queue = lookupQueue(name);
            if (queue == null) {
                if (HQDestination.isJndiName(name)) {
                    throw new IllegalArgumentException("queue " + name + " does not exist, and jndi names are lookup only.");
                } else {
                    queue = createQueue(name,
                                        opts.getString(CreateQueueOption.SELECTOR, ""),
                                        opts.getBoolean(CreateQueueOption.DURABLE));
                    this.createdDestinations.add(HQQueue.fullName(name));
                }
            } else {
                if (opts.size() > 0) {
                    log.warn("Ignoring the queue creation options provided for " + name + ", the queue already exists.");
                }
            }
        }

        return new HQQueue(name, queue, this);
    }

    @Override
    public synchronized Topic findOrCreateTopic(String name,
                                                Map<CreateTopicOption, Object> options) throws Exception {
        Options<CreateTopicOption> opts = new Options<>(options);
        javax.jms.Topic topic;
        HQSpecificContext givenContext = (HQSpecificContext)opts.get(CreateTopicOption.CONTEXT);
        if (givenContext != null) {
            if (!givenContext.isRemote()) {
                throw new IllegalArgumentException("Topic lookup only accepts a remote context.");
            }
            if (opts.size() > 1) {
                throw new IllegalArgumentException("Creation options provided for a remote topic.");
            }
            topic = givenContext.jmsContext().createTopic(name);
        } else {
            start();

            topic = lookupTopic(name);
            if (topic == null) {
                if (HQDestination.isJndiName(name)) {
                    throw new IllegalArgumentException("topic " + name + " does not exist, and jndi names are lookup only.");
                } else {
                    topic = createTopic(name);
                    this.createdDestinations.add(HQTopic.fullName(name));
                }
            } else {
                if (opts.size() > 0) {
                    log.warn("Ignoring the topic creation options provided for " + name + ", the topic already exists.");
                }
            }
        }

        return new HQTopic(name, topic, this);
    }

    protected void addCloseable(AutoCloseable closeable) {
        this.closeables.add(closeable);
    }

    protected void closeCloseables() throws Exception {
        for(AutoCloseable c : this.closeables) {
            c.close();
        }
        this.closeables.clear();
    }

    protected javax.jms.Topic createTopic(String name) throws Exception {
        this.server
                .serverManager()
                .createTopic(false, name, HQDestination.jndiName(name, "topic"));

        return lookupTopic(name);
    }

    protected javax.jms.Queue createQueue(String name, String selector, boolean durable) throws Exception {
        this.server
                .serverManager()
                .createQueue(false, name, selector, durable, HQDestination.jndiName(name, "queue"));

        return lookupQueue(name);
    }

    protected boolean destroyDestination(HQDestination dest) throws Exception {
        String fullName = dest.fullName();
        if (this.closeablesForDestination.containsKey(fullName)) {
            for(AutoCloseable each : this.closeablesForDestination.get(fullName)) {
                each.close();
            }
            this.closeablesForDestination.remove(fullName);
        }

        if (this.createdDestinations.contains(fullName)) {
            if (dest instanceof HQQueue) {
                destroyQueue(dest.name());
            } else {
                destroyTopic(dest.name());
            }
            this.createdDestinations.remove(fullName);

            return true;
        }

        return false;
    }

    protected void destroyQueue(String name) throws Exception {
        this.jmsServerManager().destroyQueue(name, true);
    }

    protected void destroyTopic(String name) throws Exception {
        this.jmsServerManager().destroyTopic(name, true);
    }

    protected void addCloseableForDestination(HQDestination dest, AutoCloseable c) {
        String fullName = dest.fullName();
        List<AutoCloseable> closeables = this.closeablesForDestination.get(fullName);
        if (closeables == null) {
            closeables = new ArrayList<>();
            this.closeablesForDestination.put(fullName, closeables);
        }

        closeables.add(c);
    }

    protected javax.jms.Topic lookupTopic(String name) {
        List<String> jndiNames = new ArrayList<>();

        if (this.server != null) {
            jndiNames.addAll(Arrays.asList(this.server.serverManager().getJNDIOnTopic(name)));
        }
        jndiNames.add(name);
        jndiNames.add(HQTopic.jmsName(name));
        jndiNames.add(HQDestination.jndiName(name, "topic"));

        return (javax.jms.Topic)lookupJNDI(jndiNames);
    }

    protected javax.jms.Queue lookupQueue(String name) {
        List<String> jndiNames = new ArrayList<>();

        if (this.server != null) {
            jndiNames.addAll(Arrays.asList(this.server.serverManager().getJNDIOnQueue(name)));
        }
        jndiNames.add(name);
        jndiNames.add(HQQueue.jmsName(name));
        jndiNames.add(HQDestination.jndiName(name, "queue"));

        return (javax.jms.Queue)lookupJNDI(jndiNames);
    }

    protected Object lookupJNDI(String jndiName) {
        return server.getRegistry().lookup(jndiName);
    }

    protected Object lookupJNDI(List<String> jndiNames) {
        for (String jndiName : jndiNames) {
            Object jndiObject = lookupJNDI(jndiName);
            if (jndiObject != null) {
                return jndiObject;
            }
        }

        return null;
    }

    private final String name;
    private final Options<CreateOption> options;
    private final Set<String> createdDestinations = new HashSet<>();
    private final Map<String, List<AutoCloseable>> closeablesForDestination = new HashMap<>();
    private final List<AutoCloseable> closeables = new ArrayList<>();
    protected boolean started = false;
    protected EmbeddedServer server;

    private final static Logger log = WunderBoss.logger("org.projectodd.wunderboss.messaging.hornetq");
}
