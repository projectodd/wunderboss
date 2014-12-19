package org.projectodd.wunderboss.web.async;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.OutputStream;

public class UndertowHttpChannel extends OutputStreamHttpChannel {
    public UndertowHttpChannel(final HttpServerExchange exchange,
                               final OnOpen onOpen,
                               final OnClose onClose) {
        super(onOpen, onClose);
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
