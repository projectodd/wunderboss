package org.projectodd.wunderboss.web.async.websocket;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;

/**
 * Created by tcrawley on 1/13/15.
 */
public class DelegatingJavaxEndpoint extends Endpoint {
    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.endpoint = (Endpoint)endpointConfig.getUserProperties().get("Endpoint");
        if (this.endpoint != null) {
            this.endpoint.onOpen(session, endpointConfig);
        } else {
            close(session);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        if (this.endpoint != null) {
            this.endpoint.onClose(session, closeReason);
        }
    }

    @Override
    public void onError(Session session, Throwable err) {
        if (this.endpoint != null) {
            this.endpoint.onError(session, err);
        } else {
            close(session);
        }
    }

    private void close(Session session) {
        try {
            session.close(POLICY_CLOSE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Endpoint endpoint;
    private final static CloseReason POLICY_CLOSE = new CloseReason(new CloseReason.CloseCode() {
        @Override
        public int getCode() {
            return 1003;
        }
    }, null);
}
