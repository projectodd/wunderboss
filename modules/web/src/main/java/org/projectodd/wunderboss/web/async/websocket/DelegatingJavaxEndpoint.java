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
