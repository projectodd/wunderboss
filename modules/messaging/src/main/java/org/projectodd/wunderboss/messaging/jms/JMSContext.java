/*
 * Copyright 2015 Red Hat, Inc, and individual contributors.
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

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.WithCloseables;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import java.util.UUID;
import java.util.concurrent.Callable;

public class JMSContext extends WithCloseables implements JMSSpecificContext {
    public static final ThreadLocal<JMSSpecificContext> currentContext = new ThreadLocal<>();

    public JMSContext(final Connection jmsConnection,
                      final Messaging broker,
                      final Mode mode,
                      final boolean remote) {
        this(jmsConnection, broker, mode, remote, null);
    }

    public JMSContext(final Connection jmsConnection,
                      final Messaging broker,
                      final Mode mode,
                      final boolean remote,
                      final JMSSpecificContext parent) {
        this.mode = mode;
        this.id = UUID.randomUUID().toString();
        this.parentContext = parent;
        this.broker = broker;
        this.remote = remote;
        this.jmsConnection = jmsConnection;

        if (parentContext != null) {
            parentContext.addCloseable(this);
        } else {
            addCloseable(jmsConnection);
        }
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public Mode mode() {
        return this.mode;
    }

    public static int modeToJMSMode(Mode mode) {
        int jmsMode = 0;
        switch (mode) {
            case AUTO_ACK:
                jmsMode = Session.AUTO_ACKNOWLEDGE;
                break;
            case CLIENT_ACK:
                jmsMode = Session.CLIENT_ACKNOWLEDGE;
                break;
            case TRANSACTED:
                jmsMode = Session.SESSION_TRANSACTED;
                break;
        }

        return jmsMode;
    }

    protected boolean isTransacted() {
        return mode() == Mode.TRANSACTED;
    }

    @Override
    public void commit() {
        if (isTransacted()) {
            DestinationUtil.mightThrow(new Callable() {
                @Override
                public Object call() throws Exception {
                    jmsSession().commit();

                    return null;
                }
            });

        }
    }

    @Override
    public void rollback() {
        if (isTransacted()) {
            DestinationUtil.mightThrow(new Callable() {
                @Override
                public Object call() throws Exception {
                    jmsSession().rollback();

                    return null;
                }
            });

        }
    }

    @Override
    public void acknowledge() {
        if (this.latestMessage != null) {
            DestinationUtil.mightThrow(new Callable() {
                @Override
                public Object call() throws Exception {
                    JMSContext.this.latestMessage.jmsMessage().acknowledge();

                    return null;
                }
            });
        }
    }

    @Override
    public void setLatestMessage(JMSMessage message) {
        this.latestMessage = message;
    }

    @Override
    public boolean enlist() throws Exception {
        if (TransactionUtil.isTransactionActive()) {
            log.warn("This non-XA context cannot participate in the active transaction; hijinks may ensue");
        }
        return false;
    }

    @Override
    public boolean isRemote() {
        return this.remote
                || (this.parentContext != null && this.parentContext.isRemote());
    }

    @Override
    public Messaging broker() {
        return this.broker;
    }

    @Override
    public boolean isChild() {
        return this.parentContext != null;
    }

    @Override
    public boolean isXAEnabled() {
        return false;
    }

    @Override
    public JMSSpecificContext asNonCloseable() {
        return this.new NonClosing();
    }

    @Override
    public JMSSpecificContext createChildContext(Mode mode) {
        return new JMSContext(this.jmsConnection, broker(), mode, isRemote(), this);
    }

    @Override
    public Connection jmsConnection() {
        return this.jmsConnection;
    }

    @Override
    public Session jmsSession() {
        if (this.jmsSession == null) {
            this.jmsSession = (Session) DestinationUtil.mightThrow(new Callable() {
                @Override
                public Object call() throws Exception {
                    jmsConnection.start();

                    return createJMSSession();
                }
            });

            addCloseable(this.jmsSession);

        }

        return this.jmsSession;
    }

    protected Session createJMSSession() throws JMSException {
        return jmsConnection().createSession(isTransacted(), modeToJMSMode(mode()));
    }

    @Override
    public void close() throws Exception {
        closeCloseables();
    }

    private final String id;
    private final Mode mode;
    private final boolean remote;
    private final JMSSpecificContext parentContext;
    private final Messaging broker;
    protected final Connection jmsConnection;
    private Session jmsSession;
    private JMSMessage latestMessage;

    private final static Logger log = Logger.getLogger(JMSContext.class);


    protected class NonClosing implements JMSSpecificContext {

        @Override
        public String id() {
            return JMSContext.this.id();
        }

        @Override
        public Mode mode() {
            return JMSContext.this.mode();
        }

        @Override
        public void commit() {
            JMSContext.this.commit();
        }

        @Override
        public void rollback() {
            JMSContext.this.rollback();
        }

        @Override
        public void acknowledge() {
            JMSContext.this.acknowledge();
        }

        @Override
        public boolean enlist() throws Exception {
            return JMSContext.this.enlist();
        }

        @Override
        public void addCloseable(Object closeable) {
            JMSContext.this.addCloseable(closeable);
        }

        @Override
        public void closeCloseables() throws Exception {
            JMSContext.this.closeCloseables();
        }

        @Override
        public boolean isRemote() {
            return JMSContext.this.isRemote();
        }

        @Override
        public void close() throws Exception {
            // Nope
        }

        @Override
        public Messaging broker() {
            return JMSContext.this.broker();
        }

        @Override
        public boolean isXAEnabled() {
            return JMSContext.this.isXAEnabled();
        }

        @Override
        public boolean isChild() {
            return JMSContext.this.isChild();
        }

        @Override
        public JMSSpecificContext asNonCloseable() {
            return this;
        }

        @Override
        public JMSSpecificContext createChildContext(Mode mode) {
            return JMSContext.this.createChildContext(mode);
        }

        @Override
        public void setLatestMessage(JMSMessage message) {
            JMSContext.this.setLatestMessage(message);
        }

        @Override
        public Connection jmsConnection() {
            return JMSContext.this.jmsConnection();
        }

        @Override
        public Session jmsSession() {
            return JMSContext.this.jmsSession();
        }
    }

}
