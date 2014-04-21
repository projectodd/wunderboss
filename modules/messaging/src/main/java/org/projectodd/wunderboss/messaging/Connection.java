package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Closeable;
import org.projectodd.wunderboss.Implementation;
import org.projectodd.wunderboss.Option;

import java.util.Map;

public interface Connection<T> extends Implementation<T>, Closeable {

    class ListenOption extends Option {
        /**
         * The client-id for durable topic subscriptions. Ignored for queues.
         */
        public static final ListenOption CLIENT_ID   = opt("client_id", ListenOption.class);
        public static final ListenOption CONCURRENCY = opt("concurrency", 1, ListenOption.class);
        /**
         * Signifies a durable topic listener. Ignored for queues.
         */
        public static final ListenOption DURABLE     = opt("durable", false, ListenOption.class);
        public static final ListenOption SELECTOR    = opt("selector", ListenOption.class);
        /**
         * subscriber-name for durable topic subscriptions. Ignored for queues. Defaults to
         * LISTENER_ID, then falls back to CLIENT_ID if LISTENER_ID not set.
         */
        public static final ListenOption SUBSCRIBER_NAME = opt("subscriber_name", ListenOption.class);
        /**
         * If true, and xa is used around the onMessage(). Defaults to whatever was specified for
         * CreateOption.XA.
         */
        public static final ListenOption XA = opt("xa", ListenOption.class);
    }

    Listener listen(Endpoint endpoint, MessageHandler handler,
                    Map<ListenOption, Object> options) throws Exception;

    Listener respond(Endpoint endpoint, MessageHandler handler,
                     Map<ListenOption, Object> options) throws Exception;

    class SendOption extends Option {
        public static final SendOption PRIORITY   = opt("priority", 4, SendOption.class); //TODO: 4 is JMS specific?
        public static final SendOption TTL        = opt("ttl", 0, SendOption.class);
        public static final SendOption PERSISTENT = opt("persistent", true, SendOption.class);
        public static final SendOption HEADERS    = opt("headers", SendOption.class);
    }

    void send(Endpoint endpoint, String content, String contentType, Map<SendOption, Object> options) throws Exception;

    void send(Endpoint endpoint, byte[] content, String contentType, Map<SendOption, Object> options) throws Exception;

    class RequestOption extends SendOption {}

    Response request(Endpoint endpoint, String content, String contentType,
                     Map<SendOption, Object> options) throws Exception;

    Response request(Endpoint endpoint, byte[] content, String contentType,
                     Map<SendOption, Object> options) throws Exception;

    class ReceiveOption extends Option {
        public static final ReceiveOption TIMEOUT  = opt("timeout", 10000, ReceiveOption.class);
        public static final ReceiveOption SELECTOR = opt("selector", ReceiveOption.class);
    }

    Message receive(Endpoint endpoint,
                    Map<ReceiveOption, Object> options) throws Exception;

}
