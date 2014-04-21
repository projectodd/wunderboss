package org.projectodd.wunderboss.messaging.hornetq;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Connection;
import org.projectodd.wunderboss.messaging.Connection.ReceiveOption;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.Response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HornetQResponse implements Response {

    public HornetQResponse(Connection connection, javax.jms.Message message, Endpoint endpoint) {
        this.connection = connection;
        this.message = message;
        this.endpoint = endpoint;
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
                // receive with connection and timeout, and selector
                this.value = this.connection.receive(this.endpoint, options);
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }

        return this.value;
    }

    private final Connection connection;
    private final javax.jms.Message message;
    private final Endpoint endpoint;
    private Message value = null;
}
