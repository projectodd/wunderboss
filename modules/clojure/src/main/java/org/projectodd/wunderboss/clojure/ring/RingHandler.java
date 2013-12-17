package org.projectodd.wunderboss.clojure.ring;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.projectodd.shimdandy.ClojureRuntimeShim;

public class RingHandler implements HttpHandler {

    public RingHandler(Object runtime, String ringFn, String context) {
        this.runtime = (ClojureRuntimeShim)runtime;
        //TODO: move interning based on FQ name to a util fn called from clojure
        String[] parts = ringFn.split("/");
        this.runtime.require(parts[0]);
        this.ringFn = this.runtime.invoke("clojure.core/intern",
                                          this.runtime.invoke("clojure.core/symbol", parts[0]),
                                          this.runtime.invoke("clojure.core/symbol", parts[1]));
        this.context = context;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        this.runtime.invoke(HANDLER_FN, this.ringFn, exchange);
    }

    private final Object ringFn;
    private final ClojureRuntimeShim runtime;
    private final String context;

    public static final String HANDLER_FN = "wunderboss.ring/handle-request";
}
