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

import org.projectodd.wunderboss.messaging.Messaging;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JMSContext implements JMSSpecificContext {

    public static ThreadLocal<JMSSpecificContext> currentContext = new ThreadLocal<>();

    public JMSContext(javax.jms.JMSContext jmsContext,
                      Messaging broker,
                      Mode mode,
                      boolean remote) {
        this(jmsContext, broker, mode, remote, null);
    }

    public JMSContext(javax.jms.JMSContext jmsContext,
                      Messaging broker,
                      Mode mode,
                      boolean remote,
                      JMSSpecificContext parent) {
        this.jmsContext = jmsContext;
        this.broker = broker;
        this.mode = mode;
        this.remote = remote;
        this.parentContext = parent;
        this.id = UUID.randomUUID().toString();

        if (parentContext != null) {
            parentContext.addCloseable(this);
        }
    }

    public static int modeToJMSMode(Mode mode) {
        int jmsMode = 0;
        switch (mode) {
            case AUTO_ACK:
                jmsMode = javax.jms.JMSContext.AUTO_ACKNOWLEDGE;
                break;
            case CLIENT_ACK:
                jmsMode = javax.jms.JMSContext.CLIENT_ACKNOWLEDGE;
                break;
            case TRANSACTED:
                jmsMode = javax.jms.JMSContext.SESSION_TRANSACTED;
                break;
        }

        return jmsMode;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public Mode mode() {
        return this.mode;
    }

    @Override
    public void commit() {
        if (this.jmsContext.getTransacted()) {
            this.jmsContext.commit();
        }
    }

    @Override
    public void rollback() {
        if (this.jmsContext.getTransacted()) {
            this.jmsContext.rollback();
        }
    }

    @Override
    public void acknowledge() {
        this.jmsContext.acknowledge();
    }

    @Override
    public boolean enlist() throws Exception {
        if (JMSXAContext.isTransactionActive()) {
            log.warn("This non-XA context cannot participate in the active transaction; hijinks may ensue");
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        for(AutoCloseable each : this.closeables) {
            each.close();
        }

        this.closeables.clear();
        this.jmsContext.close();
    }

    @Override
    public void addCloseable(AutoCloseable closeable) {
        this.closeables.add(closeable);
    }

    @Override
    public boolean isRemote() {
        return this.remote
                || (this.parentContext != null && this.parentContext.isRemote());
    }

    @Override
    public javax.jms.JMSContext jmsContext() {
        return this.jmsContext;
    }

    @Override
    public Messaging broker() {
        return this.broker;
    }

    @Override
    public boolean isXAEnabled() {
        return false;
    }

    @Override
    public boolean isChild() {
        return this.parentContext != null;
    }

    @Override
    public JMSSpecificContext asNonCloseable() {
        return this.new NonClosing();
    }

    @Override
    public JMSSpecificContext createChildContext(Mode mode) {
        javax.jms.JMSContext subContext = this.jmsContext.createContext(modeToJMSMode(mode));

        return new JMSContext(subContext, this.broker, this.mode, this.remote, this);
    }

    class NonClosing implements JMSSpecificContext {
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
        public void addCloseable(AutoCloseable closeable) {
            JMSContext.this.addCloseable(closeable);
        }

        @Override
        public boolean isRemote() {
            return JMSContext.this.isRemote();
        }

        @Override
        public void close() throws Exception {
            // Nope
        }

        public javax.jms.JMSContext jmsContext() {
            return JMSContext.this.jmsContext();
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
    }

    private final String id;
    private final Mode mode;
    private final boolean remote;
    private final javax.jms.JMSContext jmsContext;
    private final JMSSpecificContext parentContext;
    private final Messaging broker;
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private final static Logger log = Logger.getLogger(JMSContext.class);
}
