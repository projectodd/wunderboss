/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

import java.util.Map;

public class WebsocketConnectionAbortException extends RuntimeException {
    public WebsocketConnectionAbortException(final int status, final Map<String, String> headers, final String body) {
        super("Aborting websocket connection with status " + status);
        this.status = status;
        this.headers = headers;
        this.body = body;
    }


    public int status() {
        return status;
    }

    public String body() {
        return body;
    }

    public Map<String, String> headers() {
        return headers;
    }

    final private int status;
    final private String body;
    final private Map<String, String> headers;
}
