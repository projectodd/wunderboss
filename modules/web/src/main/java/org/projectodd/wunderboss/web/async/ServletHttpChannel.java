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

import org.jboss.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class ServletHttpChannel extends OutputStreamHttpChannel {

    public ServletHttpChannel(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final OnOpen onOpen,
                              final OnError onError,
                              final OnClose onClose,
                              final boolean asyncSupported){
        super(onOpen, onError, onClose);
        this.request = request;
        this.response = response;
        this.asyncSupported = asyncSupported;
    }

    private void open() {
        this.asyncContext = request.startAsync();
        this.asyncContext.setTimeout(this.timeout);
        this.asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                close();
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
            }
        });
    }

    @Override
    public boolean asyncSendSupported() {
        return this.asyncSupported;
    }
    
    @Override
    protected String getResponseCharset() {
        return this.response.getCharacterEncoding();
    }

    @Override
    protected void setContentLength(int length) {
        this.response.setContentLength(length);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return this.response.getOutputStream();
    }

    @Override
    protected void execute(Runnable runnable) {
        this.asyncContext.start(runnable);
    }

    @Override
    void enqueue(PendingSend pending) {
        //be async in 9.x, sync in 8.x (due to https://issues.jboss.org/browse/WFLY-3715)
        if (asyncSendSupported()) {
            super.enqueue(pending); // async
        } else {
            send(pending); // sync
        }
    }

    @Override
    public void notifyOpen(final Object context) {
        open();
        super.notifyOpen(context);
    }

    @Override
    public void close() throws IOException {
        this.asyncContext.complete();
        super.close();
    }

    @Override
    public void setTimeout(long timeout) {
        if (timeout >= 0) {
            if (!this.asyncSupported) {
                throw new IllegalArgumentException("HTTP stream timeouts are not supported on this platform");
            }
            this.timeout = timeout;
        }
    }

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final boolean asyncSupported;
    private long timeout = 0;
    private AsyncContext asyncContext;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.web.async");
}
