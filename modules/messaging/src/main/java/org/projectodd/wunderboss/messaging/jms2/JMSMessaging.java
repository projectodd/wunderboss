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

package org.projectodd.wunderboss.messaging.jms2;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.messaging.Context;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Queue;
import org.projectodd.wunderboss.messaging.Topic;
import org.slf4j.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class JMSMessaging implements Messaging {

    public static final String BROKER_ID = UUID.randomUUID().toString();
    public static final String JNDI_XA_CF_NAME = "java:/JmsXA";
    public static final String JNDI_CF_NAME = "java:/ConnectionFactory";

    protected void addCloseable(AutoCloseable closeable) {
        this.closeables.add(closeable);
    }

    protected void closeCloseables() throws Exception {
        for(AutoCloseable c : this.closeables) {
            c.close();
        }
        this.closeables.clear();
    }

    protected void addCloseableForDestination(JMSDestination dest, AutoCloseable c) {
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
            if (dest instanceof JMSQueue) {
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

    @Override
    public Context createContext(Map<CreateContextOption, Object> options) throws Exception {
        final Options<CreateContextOption> opts = new Options<>(options);
        JMSSpecificContext context;

        boolean xa = opts.getBoolean(CreateContextOption.XA);
        if (opts.has(CreateContextOption.HOST)) {
            if (xa) {
                context = createXAContext((XAConnectionFactory) createRemoteConnectionFactory(opts), opts);
            } else {
                context = createContext(createRemoteConnectionFactory(opts), opts);
            }
        }  else {
            start();

            if (xa) {
                context = createXAContext(JNDI_XA_CF_NAME, opts);
            } else {
                context = createContext(JNDI_CF_NAME, opts);
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
        JMSSpecificContext givenContext = (JMSSpecificContext)opts.get(CreateQueueOption.CONTEXT);
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
                if (JMSDestination.isJndiName(name)) {
                    throw new IllegalArgumentException("queue " + name + " does not exist, and jndi names are lookup only.");
                } else {
                    queue = createQueue(name,
                                        opts.getString(CreateQueueOption.SELECTOR, ""),
                                        opts.getBoolean(CreateQueueOption.DURABLE));
                    addCreatedDestination(JMSQueue.fullName(name));
                }
            } else {
                if (opts.size() > 0) {
                    log.warn("Ignoring the queue creation options provided for " + name + ", the queue already exists.");
                }
            }
        }

        return new JMSQueue(name, queue, this);
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
            topic = givenContext.jmsContext().createTopic(name);
        } else {
            start();

            topic = lookupTopic(name);
            if (topic == null) {
                if (JMSDestination.isJndiName(name)) {
                    throw new IllegalArgumentException("topic " + name + " does not exist, and jndi names are lookup only.");
                } else {
                    topic = createTopic(name);
                    addCreatedDestination(JMSTopic.fullName(name));
                }
            } else {
                if (opts.size() > 0) {
                    log.warn("Ignoring the topic creation options provided for " + name + ", the topic already exists.");
                }
            }
        }

        return new JMSTopic(name, topic, this);
    }

    protected abstract javax.jms.Queue lookupQueue(String name);

    protected abstract javax.jms.Topic lookupTopic(String name);

    protected abstract javax.jms.Queue createQueue(String name, String selector, boolean durable) throws Exception;

    protected abstract javax.jms.Topic createTopic(String name) throws Exception;

    protected abstract void destroyQueue(String name) throws Exception;

    protected abstract void destroyTopic(String name) throws Exception;

    protected abstract Object lookupJNDI(String name);

    protected abstract ConnectionFactory createRemoteConnectionFactory(final Options<CreateContextOption> options);


    private JMSSpecificContext createContext(ConnectionFactory cf, Options<CreateContextOption> options) {
        int mode = JMSContext.modeToJMSMode((Context.Mode) options.get(CreateContextOption.MODE));
        javax.jms.JMSContext jmsContext;

        if (options.has(CreateContextOption.USERNAME)) {
            jmsContext = cf.createContext(options.getString(CreateContextOption.USERNAME),
                                          options.getString(CreateContextOption.PASSWORD),
                                          mode);
        } else {
            jmsContext = cf.createContext(mode);
        }

        return new JMSContext(jmsContext, this,
                                     (Context.Mode)options.get(CreateContextOption.MODE),
                                     options.has(CreateContextOption.HOST));
    }

    private JMSSpecificContext createContext(String factoryName, Options<CreateContextOption> options) {
        return createContext((ConnectionFactory) lookupJNDI(factoryName), options);
    }

    private JMSSpecificContext createXAContext(XAConnectionFactory cf, Options<CreateContextOption> options) {
        XAJMSContext context;

        if (JMSXAContext.tm == null) {
            throw new NullPointerException("TransactionManager not found; is transactions module on the classpath?");
        }

        if (options.has(CreateContextOption.USERNAME)) {
            context = cf.createXAContext(options.getString(CreateContextOption.USERNAME),
                                         options.getString(CreateContextOption.PASSWORD));
        } else {
            context = cf.createXAContext();
        }

        return new JMSXAContext(context, this,
                               (Context.Mode)options.get(CreateContextOption.MODE),
                               options.has(CreateContextOption.HOST));
    }

    private JMSSpecificContext createXAContext(String factoryName, Options<CreateContextOption> options) {
        return createXAContext((XAConnectionFactory) lookupJNDI(factoryName), options);
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

    private final Map<String, List<AutoCloseable>> closeablesForDestination = new HashMap<>();
    private final Set<String> createdDestinations = new HashSet<>();
    private final List<AutoCloseable> closeables = new ArrayList<>();

    private final static Logger log = WunderBoss.logger("org.projectodd.wunderboss.messaging.jms");


}
