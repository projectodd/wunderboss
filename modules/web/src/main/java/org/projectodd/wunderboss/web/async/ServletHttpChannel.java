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
import org.projectodd.wunderboss.WunderBoss;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class ServletHttpChannel extends OutputStreamHttpChannel {

    public ServletHttpChannel(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final OnOpen onOpen,
                              final OnError onError,
                              final OnClose onClose){
        super(onOpen, onError, onClose);
        this.response = response;
        this.asyncContext = request.startAsync();
        this.asyncContext.setTimeout(0);
    }

    @Override
    public boolean asyncSendSupported() {
        return isAsync();
    }
    
    @Override
    protected String getResponseCharset() {
        return this.response.getCharacterEncoding();
    }

    @Override
    protected void setContentLength(int length) {
        this.response.setIntHeader("Content-Length", length);
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return this.response.getOutputStream();
    }

    @Override
    protected void execute(Runnable runnable) {
        this.asyncContext.start(runnable);
    }

    static boolean isAsync() {
        if (asyncSupported == null) {
            asyncSupported = false;
            String version = WunderBoss.options().getString("wildfly-version", "");
            String[] parts = version.split("\\.");
            if (parts.length > 0) {
                try {
                    if (Integer.parseInt(parts[0]) >= 9) {
                        asyncSupported = true;
                    }
                } catch (NumberFormatException _) {
                }
            }

            if (!asyncSupported) {
                log.warn("NOTE: HTTP stream sends are synchronous in WildFly " + version +
                                 ". Use 9.0.0.Alpha1 or higher to have asynchronous sends.");
            }
        }

        return asyncSupported;
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
    public void close() throws IOException {
        this.asyncContext.complete();
        super.close();
    }

    private final HttpServletResponse response;
    private final AsyncContext asyncContext;
    private static Boolean asyncSupported;

    private static final Logger log = Logger.getLogger("org.projectodd.wunderboss.web.async");
}
