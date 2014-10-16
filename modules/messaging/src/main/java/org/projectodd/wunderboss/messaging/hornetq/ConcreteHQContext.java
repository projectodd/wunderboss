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

import org.projectodd.wunderboss.messaging.Messaging;

import javax.jms.JMSContext;
import java.util.ArrayList;
import java.util.List;

public class ConcreteHQContext implements HQContext {

    public static ThreadLocal<HQContext> currentContext = new ThreadLocal<>();

    public ConcreteHQContext(JMSContext jmsContext,
                             Messaging broker,
                             Mode mode,
                             boolean remote) {
        this(jmsContext, broker, mode, remote, null);
    }

    public ConcreteHQContext(JMSContext jmsContext,
                             Messaging broker,
                             Mode mode,
                             boolean remote,
                             HQContext parent) {
        this.jmsContext = jmsContext;
        this.broker = broker;
        this.mode = mode;
        this.remote = remote;
        this.parentContext = parent;

        if (parentContext != null) {
            parentContext.addCloseable(this);
        }
    }

    public static int modeToJMSMode(Mode mode) {
        int jmsMode = 0;
        switch (mode) {
            case AUTO_ACK:
                jmsMode = JMSContext.AUTO_ACKNOWLEDGE;
                break;
            case CLIENT_ACK:
                jmsMode = JMSContext.CLIENT_ACKNOWLEDGE;
                break;
            case TRANSACTED:
                jmsMode = JMSContext.SESSION_TRANSACTED;
                break;
        }

        return jmsMode;
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
    public JMSContext jmsContext() {
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
    public HQContext asNonCloseable() {
        return this.new NonClosing();
    }

    @Override
    public HQContext createChildContext(Mode mode) {
        JMSContext subContext = this.jmsContext.createContext(modeToJMSMode(mode));

        return new ConcreteHQContext(subContext, this.broker, this.mode, this.remote, this);
    }

    class NonClosing implements HQContext {
        @Override
        public Mode mode() {
            return ConcreteHQContext.this.mode();
        }

        @Override
        public void commit() {
            ConcreteHQContext.this.commit();
        }

        @Override
        public void rollback() {
            ConcreteHQContext.this.rollback();
        }

        @Override
        public void acknowledge() {
            ConcreteHQContext.this.acknowledge();
        }

        @Override
        public boolean enlist() throws Exception {
            return ConcreteHQContext.this.enlist();
        }

        @Override
        public void addCloseable(AutoCloseable closeable) {
            ConcreteHQContext.this.addCloseable(closeable);
        }

        @Override
        public boolean isRemote() {
            return ConcreteHQContext.this.isRemote();
        }

        @Override
        public void close() throws Exception {
            // Nope
        }

        public JMSContext jmsContext() {
            return ConcreteHQContext.this.jmsContext();
        }

        @Override
        public Messaging broker() {
            return ConcreteHQContext.this.broker();
        }

        @Override
        public boolean isXAEnabled() {
            return ConcreteHQContext.this.isXAEnabled();
        }

        @Override
        public boolean isChild() {
            return ConcreteHQContext.this.isChild();
        }

        @Override
        public HQContext asNonCloseable() {
            return this;
        }

        @Override
        public HQContext createChildContext(Mode mode) {
            return ConcreteHQContext.this.createChildContext(mode);
        }
    }

    private final Mode mode;
    private final boolean remote;
    private final JMSContext jmsContext;
    private final HQContext parentContext;
    private final Messaging broker;
    private final List<AutoCloseable> closeables = new ArrayList<>();
}
