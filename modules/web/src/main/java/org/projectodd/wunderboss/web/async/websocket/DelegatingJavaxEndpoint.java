/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.web.async.websocket;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;

public class DelegatingJavaxEndpoint extends Endpoint {
    public static final String ENDPOINT_KEY = "session-endpoint";

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        Endpoint endpoint = sessionEndpoint(session);
        if (endpoint != null) {
            endpoint.onOpen(session, endpointConfig);
        } else {
            close(session);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        Endpoint endpoint = sessionEndpoint(session);
        if (endpoint != null) {
            endpoint.onClose(session, closeReason);
        }
    }

    @Override
    public void onError(Session session, Throwable err) {
        Endpoint endpoint = sessionEndpoint(session);
        if (endpoint != null) {
            endpoint.onError(session, err);
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

    private Endpoint sessionEndpoint(final Session session) {
        return (Endpoint)session.getUserProperties().get(ENDPOINT_KEY);
    }

    private final static CloseReason POLICY_CLOSE = new CloseReason(new CloseReason.CloseCode() {
        @Override
        public int getCode() {
            return 1003;
        }
    }, null);
}
