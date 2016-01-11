/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.messaging.jms;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Queue;
import org.projectodd.wunderboss.messaging.Topic;
import org.projectodd.wunderboss.messaging.WithCloseables;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

public abstract class JMSMessagingSkeleton extends WithCloseables implements Messaging {

    public static final String BROKER_ID = UUID.randomUUID().toString();
    public static final String JNDI_XA_CF_NAME = "java:/JmsXA";
    public static final String JNDI_CF_NAME = "java:/ConnectionFactory";

    protected abstract javax.jms.Queue lookupQueue(String name);

    protected abstract javax.jms.Topic lookupTopic(String name);

    protected abstract javax.jms.Queue createQueue(String name, String selector, boolean durable) throws Exception;

    protected abstract javax.jms.Topic createTopic(String name) throws Exception;

    protected abstract void destroyQueue(String name) throws Exception;

    protected abstract void destroyTopic(String name) throws Exception;

    protected abstract Object lookupJNDI(String name);

    protected abstract ConnectionFactory createRemoteConnectionFactory(final Options<CreateContextOption> options);

    protected JMSSpecificContext createContext(final ConnectionFactory cf, final Options<CreateContextOption> options) {
        Connection connection = (Connection) DestinationUtil.mightThrow(new Callable() {
            @Override
            public Object call() throws Exception {
                Connection connection;
                if (options.has(CreateContextOption.USERNAME)) {
                    connection = cf.createConnection(options.getString(CreateContextOption.USERNAME),
                                                     options.getString(CreateContextOption.PASSWORD));

                } else {
                    connection = cf.createConnection();
                }

                return connection;
            }
        });



        return new JMSContext(connection, this,
                                     (Context.Mode)options.get(CreateContextOption.MODE),
                                     options.has(CreateContextOption.HOST));
    }

    protected javax.jms.Queue createRemoteQueue(final JMSSpecificContext context, final String name) {
        return (javax.jms.Queue) DestinationUtil.mightThrow(new Callable() {
            @Override
            public Object call() throws Exception {
                return context.jmsSession().createQueue(name);
            }
        });
    }

    protected Queue queueWrapper(String name, javax.jms.Queue queue, Messaging broker) {
        return new JMSQueue(name, queue, (JMSMessagingSkeleton)broker);
    }

    protected javax.jms.Topic createRemoteTopic(final JMSSpecificContext context, final String name) {
        return (javax.jms.Topic) DestinationUtil.mightThrow(new Callable() {
            @Override
            public Object call() throws Exception {
                return context.jmsSession().createTopic(name);
            }
        });
    }

    protected Topic topicWrapper(String name, javax.jms.Topic topic, Messaging broker) {
        return new JMSTopic(name, topic, (JMSMessagingSkeleton)broker);
    }

    public void addCloseableForDestination(JMSDestination dest, AutoCloseable c) {
        String fullName = dest.fullName();
        List<AutoCloseable> closeables = this.closeablesForDestination.get(fullName);
        if (closeables == null) {
            closeables = new ArrayList<>();
            this.closeablesForDestination.put(fullName, closeables);
        }

        closeables.add(c);
    }

    protected boolean destroyDestination(JMSDestination dest) throws Exception {
        String fullName = dest.fullName();
        if (this.closeablesForDestination.containsKey(fullName)) {
            for(AutoCloseable each : this.closeablesForDestination.get(fullName)) {
                each.close();
            }
            this.closeablesForDestination.remove(fullName);
        }

        if (this.createdDestinations.contains(fullName)) {
            if (dest.type() == JMSDestination.Type.QUEUE) {
                destroyQueue(dest.name());
            } else {
                destroyTopic(dest.name());
            }
            this.createdDestinations.remove(fullName);

            return true;
        }

        return false;
    }

    protected void addCreatedDestination(String name) {
        this.createdDestinations.add(name);
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

    @Override
    public Context createContext(Map<CreateContextOption, Object> options) throws Exception {
        final Options<CreateContextOption> opts = new Options<>(options);
        JMSSpecificContext context;

        boolean xa = opts.getBoolean(CreateContextOption.XA);
        ConnectionFactory cf;
        if (opts.has(CreateContextOption.HOST)) {
            cf = createRemoteConnectionFactory(opts);

        }  else {
            start();

            cf = (ConnectionFactory)lookupJNDI(xa ? JNDI_XA_CF_NAME : JNDI_CF_NAME);
        }

        if (xa) {
            context = createXAContext((XAConnectionFactory)cf, opts);
        } else {
            context = createContext(cf, opts);
        }

        if (opts.has(CreateContextOption.CLIENT_ID)) {
            context.jmsConnection().setClientID(opts.getString(CreateContextOption.CLIENT_ID));
        }

        return context;
    }

    protected JMSSpecificContext createXAContext(final XAConnectionFactory cf, final Options<CreateContextOption> options) {

        if (TransactionUtil.tm == null) {
            throw new NullPointerException("TransactionManager not found; is transactions module on the classpath?");
        }

        XAConnection connection = (XAConnection) DestinationUtil.mightThrow(new Callable() {
            @Override
            public Object call() throws Exception {
                if (options.has(CreateContextOption.USERNAME)) {
                    return cf.createXAConnection(options.getString(CreateContextOption.USERNAME),
                                                 options.getString(CreateContextOption.PASSWORD));
                } else {
                    return cf.createXAConnection();
                }
            }
        });

        return new JMSXAContext(connection, this,
                                (Context.Mode)options.get(CreateContextOption.MODE),
                                options.has(CreateContextOption.HOST));
    }

    @Override
    public synchronized Queue findOrCreateQueue(String name,
                                                Map<CreateQueueOption, Object> options) throws Exception {
        Options<CreateQueueOption> opts = new Options<>(options);
        javax.jms.Queue queue;
        JMSSpecificContext givenContext = (JMSSpecificContext)opts.get(CreateQueueOption.CONTEXT);
        if (givenContext != null) {
            if (!givenContext.isRemote()) {
                throw new IllegalArgumentException("Queue lookup only accepts a remote context.");
            }
            if (opts.size() > 1) {
                throw new IllegalArgumentException("Creation options provided for a remote queue.");
            }
            queue = createRemoteQueue(givenContext, name);
        } else {
            start();

            queue = lookupQueue(name);
            if (queue == null) {
                if (DestinationUtil.isJndiName(name)) {
                    throw new IllegalArgumentException("queue " + name + " does not exist, and jndi names are lookup only.");
                } else {
                    queue = createQueue(name,
                                        opts.getString(CreateQueueOption.SELECTOR, ""),
                                        opts.getBoolean(CreateQueueOption.DURABLE));
                    addCreatedDestination(DestinationUtil.fullName(name, JMSDestination.Type.QUEUE));
                }
            } else {
                if (opts.size() > 0) {
                    log.warn("Ignoring the queue creation options provided for " + name + ", the queue already exists.");
                }
            }
        }

        return queueWrapper(name, queue, this);
    }

    @Override
    public synchronized Topic findOrCreateTopic(String name,
                                                Map<CreateTopicOption, Object> options) throws Exception {
        Options<CreateTopicOption> opts = new Options<>(options);
        javax.jms.Topic topic;
        JMSSpecificContext givenContext = (JMSSpecificContext)opts.get(CreateTopicOption.CONTEXT);
        if (givenContext != null) {
            if (!givenContext.isRemote()) {
                throw new IllegalArgumentException("Topic lookup only accepts a remote context.");
            }
            if (opts.size() > 1) {
                throw new IllegalArgumentException("Creation options provided for a remote topic.");
            }
            topic = createRemoteTopic(givenContext, name);
        } else {
            start();

            topic = lookupTopic(name);
            if (topic == null) {
                if (DestinationUtil.isJndiName(name)) {
                    throw new IllegalArgumentException("topic " + name + " does not exist, and jndi names are lookup only.");
                } else {
                    topic = createTopic(name);
                    addCreatedDestination(DestinationUtil.fullName(name, JMSDestination.Type.TOPIC));
                }
            } else {
                if (opts.size() > 0) {
                    log.warn("Ignoring the topic creation options provided for " + name + ", the topic already exists.");
                }
            }
        }

        return topicWrapper(name, topic, this);
    }

    private final Map<String, List<AutoCloseable>> closeablesForDestination = new HashMap<>();
    private final Set<String> createdDestinations = new HashSet<>();

    private final static Logger log = WunderBoss.logger("org.projectodd.wunderboss.messaging.jms");


}
