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

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Messaging;

import javax.jms.JMSContext;
import java.util.ArrayList;
import java.util.List;

public class ConcreteHQContext implements HQContext {

    public static ThreadLocal<HQContext> currentContext = new ThreadLocal<>();

    public ConcreteHQContext(JMSContext jmsContext,
                             Messaging broker,
                             Options<Messaging.CreateContextOption> creationOptions,
                             HQContext parent) {
        this.jmsContext = jmsContext;
        this.broker = broker;
        this.creationOptions = creationOptions;
        this.mode = (Mode)creationOptions.get(Messaging.CreateContextOption.MODE);
        this.parentContext = parent;

        if (parentContext != null) {
            parentContext.addCloseable(this);
        }
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
        return this.creationOptions.has(Messaging.CreateContextOption.HOST)
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
        return this.creationOptions.getBoolean(Messaging.CreateContextOption.XA);
    }

    @Override
    public boolean isChild() {
        return this.parentContext != null;
    }

    @Override
    public HQContext asNonCloseable() {
        return this.new NonClosing();
    }

    public Options<Messaging.CreateContextOption> creationOptions() {
        return this.creationOptions;
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
    }

    private final Mode mode;
    private final JMSContext jmsContext;
    private final HQContext parentContext;
    private final Messaging broker;
    private final Options<Messaging.CreateContextOption> creationOptions;
    private final List<AutoCloseable> closeables = new ArrayList<>();
}
