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

package org.projectodd.wunderboss.messaging;

import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.codecs.Codecs;

import java.util.HashMap;
import java.util.Map;

public class ResponseRouter implements AutoCloseable, MessageHandler {

    public ResponseRouter(String id) {
        this.id = id;
    }

    @Override
    public Reply onMessage(Message msg, Context ignored) throws Exception {
        String id = msg.requestID();
        Response response = this.responses.remove(id);
        if (response == null) {
            throw new IllegalStateException("No responder for id " + id);
        }
        response.deliver(msg);

        return null;
    }


    public void registerResponse(String id, Response response) {
        this.responses.put(id, response);
    }


    public synchronized static ResponseRouter routerFor(Queue queue, Codecs codecs,
                                                        Options<Destination.ListenOption> options) {
        String id = queue.name();
        Context givenContext = (Context)options.get(Destination.ListenOption.CONTEXT);
        if (givenContext != null) {
            id += ":" + givenContext.id();
        }
        ResponseRouter router = routers.get(id);
        if (router == null) {
            router = new ResponseRouter(id);
            try {
                router.setEnclosingListener(queue.listen(router, codecs, options));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (givenContext != null) {
                givenContext.addCloseable(router);
            }
            queue.addCloseable(router);
            routers.put(router.id(), router);
        }

        return router;
    }

    @Override
    public void close() throws Exception {
        routers.remove(id());

        if (this.enclosingListener != null) {
            this.enclosingListener.close();
        }
    }

    public String id() {
        return id;
    }

    protected void setEnclosingListener(Listener l) {
        this.enclosingListener = l;
    }

    private final static Map<String, ResponseRouter> routers = new HashMap<>();
    private final Map<String, Response> responses = new HashMap<>();
    private final String id;
    private Listener enclosingListener;


}
