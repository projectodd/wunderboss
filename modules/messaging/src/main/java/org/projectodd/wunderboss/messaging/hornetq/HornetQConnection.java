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

import javax.jms.JMSContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HornetQConnection implements org.projectodd.wunderboss.messaging.Connection {

    public HornetQConnection(JMSContext jmsContext, Messaging broker,
                             Options<Messaging.CreateConnectionOption> creationOptions) {
        this.jmsContext = jmsContext;
        this.broker = broker;
        this.creationOptions = creationOptions;
    }

    @Override
    public Session createSession(Map<CreateSessionOption, Object> options) throws Exception {
        Options<CreateSessionOption> opts = new Options<>(options);
        Session.Mode optMode = (Session.Mode)opts.get(CreateSessionOption.MODE);
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

        return new HornetQSession(session, optMode);
    }

    @Override
    public void close() throws Exception {
        for(AutoCloseable each : this.closeables) {
            each.close();
        }
        this.closeables.clear();
        this.jmsContext.close();
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

    protected void addCloseable(AutoCloseable closeable) {
        this.closeables.add(closeable);
    }

    private final JMSContext jmsContext;
    private final Messaging broker;
    private final Options<Messaging.CreateConnectionOption> creationOptions;
    private final List<AutoCloseable> closeables = new ArrayList<>();


}
