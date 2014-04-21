package org.projectodd.wunderboss.messaging.hornetq;

import org.projectodd.wunderboss.Closeable;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.messaging.Endpoint;
import org.projectodd.wunderboss.messaging.Listener;
import org.projectodd.wunderboss.messaging.Message;
import org.projectodd.wunderboss.messaging.MessageHandler;
import org.projectodd.wunderboss.messaging.Messaging;
import org.projectodd.wunderboss.messaging.Response;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HornetQConnection implements org.projectodd.wunderboss.messaging.Connection<Connection> {
    public static final String CONTENT_TYPE_PROPERTY = "contentType";
    protected static final String SYNC_ATTRIBUTE = "synchronous";

    public HornetQConnection(Messaging parent, Connection jmsConnection) {
        this.jmsConnection = jmsConnection;
        this.parent = parent;
    }

    @Override
    public Listener listen(Endpoint endpoint, MessageHandler handler,
                           Map<ListenOption, Object> options) throws Exception {
        Options<ListenOption> opts = new Options<>(options);
        Listener listener = new MessageHandlerGroup(this.parent, handler,
                                                    endpoint,
                                                    opts).start();

        this.listeners.add(listener);

        return listener;
    }

    @Override
    public Listener respond(Endpoint endpoint, MessageHandler handler,
                            Map<ListenOption, Object> options) throws Exception {
        Options<ListenOption> opts = new Options<>(options);
        String selector = SYNC_ATTRIBUTE + " = TRUE";
        if (opts.has(ListenOption.SELECTOR)) {
            selector += " AND " + opts.getString(ListenOption.SELECTOR);
        }
        opts.put(ListenOption.SELECTOR, selector);

        return listen(endpoint, handler, opts);
    }

    @Override
    public void send(Endpoint endpoint, String content, String contentType,
                     Map<SendOption, Object> options) throws Exception {
        //TODO: transacted, ack mode?
        try (Session session = createSession()) {
            send(session, endpoint, buildTextMessage(session, content), contentType, options);
        }
    }

    @Override
    public void send(Endpoint endpoint, byte[] content, String contentType,
                     Map<SendOption, Object> options) throws Exception {
        //TODO: transacted, ack mode?
        try (Session session = createSession()) {
            send(session, endpoint, buildBytesMessage(session, content), contentType, options);
        }
    }

    protected static TextMessage buildTextMessage(Session session, String content) throws JMSException {
        return session.createTextMessage(content);
    }

    protected static BytesMessage buildBytesMessage(Session session, byte[] bytes) throws JMSException {
        BytesMessage message = session.createBytesMessage();
        message.writeBytes(bytes);

        return message;
    }

    protected static javax.jms.Message fillInHeaders(javax.jms.Message message, Map<String, Object> headers) throws JMSException {
        for(Map.Entry<String, Object> each : headers.entrySet()) {
            message.setObjectProperty(each.getKey(), each.getValue());
        }
        return message;
    }

    protected Session createSession() throws Exception {
        return this.jmsConnection.createSession();
    }

    protected void send(Session session, Endpoint<Destination> endpoint,
                        javax.jms.Message message, String contentType,
                        Map<SendOption, Object> options) throws Exception {
        Options<SendOption> opts = new Options<>(options);
        message.setStringProperty(CONTENT_TYPE_PROPERTY, contentType);
        fillInHeaders(message, (Map<String, Object>)opts.get(SendOption.HEADERS, Collections.emptyMap()));
        MessageProducer producer = session.createProducer(endpoint.implementation());
        producer.send(message,
                      (opts.getBoolean(SendOption.PERSISTENT, (Boolean)SendOption.PERSISTENT.defaultValue) ?
                              DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT),
                      opts.getInt(SendOption.PRIORITY, (Integer)SendOption.PRIORITY.defaultValue),
                      opts.getLong(SendOption.TTL, producer.getTimeToLive()));
    }

    @Override
    public Response request(Endpoint endpoint, String content, String contentType,
                            Map<SendOption, Object> options) throws Exception {
        javax.jms.Message message;
        try (Session session = createSession()) {
            message = buildTextMessage(session, content);
            message.setBooleanProperty(SYNC_ATTRIBUTE, true);
            send(session, endpoint, message, contentType, options);
        }

        return new HornetQResponse(this, message, endpoint);
    }

    @Override
    public Response request(Endpoint endpoint, byte[] content, String contentType,
                            Map<SendOption, Object> options) throws Exception {
        javax.jms.Message message;
        try (Session session = createSession()) {
            message = buildBytesMessage(session, content);
            message.setBooleanProperty(SYNC_ATTRIBUTE, true);
            send(session, endpoint, message, contentType, options);
        }

        return new HornetQResponse(this, message, endpoint);
    }

    @Override
    public Message receive(Endpoint endpoint, Map<ReceiveOption, Object> options) throws Exception {
        Options<ReceiveOption> opts = new Options<>(options);
        int timeout = opts.getInt(ReceiveOption.TIMEOUT, (Integer)ReceiveOption.TIMEOUT.defaultValue);
        try (Session session = this.jmsConnection.createSession()) {
            javax.jms.Message message = session.createConsumer((Destination)endpoint.implementation())
                    .receive(timeout);

            if (message != null) {
                return new HornetQMessage(message, endpoint, this);
            } else {
                return null;
            }
        }
    }

    @Override
    public void close() throws Exception {
        for(Closeable each : this.listeners) {
            each.close();
        }
        this.jmsConnection.close();
    }

    @Override
    public Connection implementation() {
        return this.jmsConnection;
    }

    private final Connection jmsConnection;
    private final Messaging parent;
    private final List<Listener> listeners = new ArrayList<>();
}
