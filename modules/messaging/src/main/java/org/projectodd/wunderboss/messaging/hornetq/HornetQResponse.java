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
import org.projectodd.wunderboss.messaging.Destination;
import org.projectodd.wunderboss.messaging.Destination.ReceiveOption;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.Response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HornetQResponse implements Response {

    public HornetQResponse(String requestId, Destination destination, HornetQConnection connection) {
        this.requestId = requestId;
        this.destination = destination;
        this.connection = connection;
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
                Options<Destination.MessageOpOption> options = new Options<>();
                options.put(ReceiveOption.TIMEOUT, unit.toMillis(timeout));
                options.put(ReceiveOption.SELECTOR, "JMSCorrelationID='" + this.requestId + "'");
                options.put(ReceiveOption.CONNECTION, this.connection);

                this.value = this.destination.receive(options);
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

    private final HornetQConnection connection;
    private final String requestId;
    private final Destination destination;
    private Message value = null;
}
