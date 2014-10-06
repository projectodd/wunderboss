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
import org.projectodd.wunderboss.messaging.Session;
import org.projectodd.wunderboss.messaging.Connection;

import javax.jms.JMSContext;
import javax.transaction.Synchronization;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HornetQConnection implements Connection, Synchronization {

    public HornetQConnection(JMSContext jmsContext, Messaging broker,
                             Options<Messaging.CreateConnectionOption> creationOptions) {
        this.jmsContext = jmsContext;
        this.broker = broker;
        this.creationOptions = creationOptions;
    }

    @Override
    public Session createSession(Map<CreateSessionOption, Object> options) throws Exception {
        return createSession(options, this);
    }

    @Override
    public void close() throws Exception {
        if (isXAEnabled() && HornetQXASession.tm.getTransaction() != null) {
            HornetQXASession.tm.getTransaction().registerSynchronization(this);
        } else {
            shutItDown();
        }
    }

    @Override
    public void addCloseable(AutoCloseable closeable) {
        this.closeables.add(closeable);
    }

    @Override
    public void afterCompletion(int status) {
        try {
            shutItDown();
        } catch (Exception e) {
            throw new RuntimeException("Error after tx complete", e);
        }
    }
    @Override
    public void beforeCompletion() {
        // nothing
    }

    public JMSContext jmsContext() {
        return this.jmsContext;
    }

    public Messaging broker() {
        return this.broker;
    }

    public boolean isXAEnabled() {
        return this.creationOptions.getBoolean(Messaging.CreateConnectionOption.XA);
    }

    public Options<Messaging.CreateConnectionOption> creationOptions() {
        return this.creationOptions;
    }

    void shutItDown() throws Exception {
        for(AutoCloseable each : this.closeables) {
            each.close();
        }
        this.closeables.clear();
        this.jmsContext.close();
    }

    Session createSession(Map<CreateSessionOption, Object> options, Connection conn) throws Exception {
        Options<CreateSessionOption> opts = new Options<>(options);
        Session.Mode optMode = (Session.Mode)opts.get(CreateSessionOption.MODE);
        // TODO: really should be testing conn here, or better yet,
        // introduce HQXAConnection?
        if (isXAEnabled()) {
            return new HornetQXASession(conn, this.jmsContext, optMode);
        } else {
            int mode = 0;
            switch (optMode) {
            case AUTO_ACK:
                mode = JMSContext.AUTO_ACKNOWLEDGE;
                break;
            case CLIENT_ACK:
                mode = JMSContext.CLIENT_ACKNOWLEDGE;
                break;
            case TRANSACTED:
                mode = JMSContext.SESSION_TRANSACTED;
                break;
            }
            JMSContext session = this.jmsContext.createContext(mode);
            this.closeables.add(session);
            return new HornetQSession(conn, session, optMode);
        }
    }

    class NonClosing implements Connection {
        @Override
        public Session createSession(Map<CreateSessionOption, Object> options) throws Exception {
            return HornetQConnection.this.createSession(options, this);
        }

        @Override
        public void addCloseable(AutoCloseable closeable) {
            HornetQConnection.this.addCloseable(closeable);
        }

        @Override
        public void close() throws Exception {
            // Nope
        }
    }

    private final JMSContext jmsContext;
    private final Messaging broker;
    private final Options<Messaging.CreateConnectionOption> creationOptions;
    private final List<AutoCloseable> closeables = new ArrayList<>();


}
