package org.projectodd.wunderboss.clojure.ring;

import clojure.api.API;
import clojure.lang.IFn;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class RingHandler implements HttpHandler {

    public RingHandler(IFn ringFn, String context) {
        this.ringFn = ringFn;
        API.var("clojure.core", "require").invoke(API.read(HANDLER_FN_NS));
        this.handlerFn = API.var(HANDLER_FN_NS, HANDLER_FN_NAME);
        this.context = context;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        this.handlerFn.invoke(this.ringFn, exchange);
    }

    private IFn ringFn;
    private String context;
    private IFn handlerFn;

    public static final String HANDLER_FN_NS = "wunderboss.ring";
    public static final String HANDLER_FN_NAME = "handle-request";
}
