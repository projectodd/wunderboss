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

import io.undertow.util.Headers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

public class ServletHttpChannel extends OutputStreamHttpChannel {

    public ServletHttpChannel(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final OnOpen onOpen,
                              final OnClose onClose){
        super(onOpen, onClose);
        this.response = response;
        request.startAsync();
    }

    @Override
    protected String getResponseCharset() {
        return this.response.getCharacterEncoding();
    }

    @Override
    protected void setContentLength(int length) {
        this.response.setIntHeader(Headers.CONTENT_LENGTH_STRING,
                                   length);
    }

    @Override
    protected OutputStream getOutputStream() throws Exception {
        return this.response.getOutputStream();
    }

    private final HttpServletResponse response;
}
