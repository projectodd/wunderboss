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

package org.projectodd.wunderboss.web.async;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.OutputStream;

public class UndertowHttpChannel extends OutputStreamHttpChannel {
    public UndertowHttpChannel(final HttpServerExchange exchange,
                               final OnOpen onOpen,
                               final OnError onError,
                               final OnClose onClose) {
        super(onOpen, onError, onClose);
        this.exchange = exchange.setPersistent(true).dispatch();
    }

    @Override
    protected String getResponseCharset() {
        // getResponseCharset claims to return ISO-8859-1 if a charset can't be found,
        // but returns null instead if content-type isn't set
        String charset = this.exchange.getResponseCharset();
        if (charset == null) {
            charset = "ISO-8859-1";
        }

        return charset;
    }

    @Override
    protected void setContentLength(int length) {
        this.exchange.getResponseHeaders().add(Headers.CONTENT_LENGTH,
                                               length);
    }

    @Override
    protected OutputStream getOutputStream() {
        return this.exchange.getOutputStream();
    }

    private final HttpServerExchange exchange;
}
