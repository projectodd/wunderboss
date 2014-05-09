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
import org.projectodd.wunderboss.messaging.Connection;
import org.projectodd.wunderboss.messaging.Connection.ReceiveOption;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HornetQResponse implements Response {

    public HornetQResponse(javax.jms.Message message, Endpoint endpoint, Messaging broker,
                           Options<Messaging.CreateConnectionOption> connectionOptions) {
        this.message = message;
        this.endpoint = endpoint;
        this.broker = broker;
        this.connectionOptions = connectionOptions;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("HornetQ response futures can't be cancelled.");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return (this.value != null);
    }

    @Override
    public Message get() throws InterruptedException, ExecutionException {
        try {
            return get(0, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            //shouldn't happen
            e.printStackTrace();

            return null;
        }
    }

    @Override
    public Message get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!isDone()) {
            try {
                Options<ReceiveOption> options = new Options<>();
                options.put(ReceiveOption.TIMEOUT, unit.toMillis(timeout));
                options.put(ReceiveOption.SELECTOR, "JMSCorrelationID='" + this.message.getJMSMessageID() + "'");
                try (Connection connection = this.broker.createConnection(this.connectionOptions)) {
                    this.value = connection.receive(this.endpoint, options);
                }
            } catch (Exception e) {
                throw new ExecutionException(e);
            }

            // receive will return null when it times out, so we need to pass that timeout on
            if (this.value == null) {
                throw new TimeoutException();
            }
        }

        return this.value;
    }

    private final Messaging broker;
    private final Options<Messaging.CreateConnectionOption> connectionOptions;
    private final javax.jms.Message message;
    private final Endpoint endpoint;
    private Message value = null;
}
