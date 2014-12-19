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
