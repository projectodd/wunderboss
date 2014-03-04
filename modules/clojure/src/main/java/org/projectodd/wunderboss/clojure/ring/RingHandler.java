package org.projectodd.wunderboss.clojure.ring;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.projectodd.wunderboss.LoaderWrapper;

import java.util.concurrent.Callable;

public class RingHandler implements HttpHandler {

    public RingHandler(final LoaderWrapper runtime, final String ringFn, final String context) {
        this.runtime = runtime;
        this.context = context;

        runtime.callInLoader(new Callable() {
            @Override
            public Object call() throws Exception {
                //TODO: move interning based on FQ name to a util fn called from clojure
                String[] parts = ringFn.split("/");
                IFn symbol = Clojure.var("clojure.core/symbol");
                Object nsSym = symbol.invoke(parts[0]);
                IFn require = Clojure.var("clojure.core/require");
                require.invoke(nsSym);

                RingHandler.this.ringFn = Clojure.var("clojure.core/intern").invoke(nsSym, symbol.invoke(parts[1]));

                require.invoke(symbol.invoke("wunderboss.ring"));
                RingHandler.this.handlerFn = Clojure.var("wunderboss.ring/handle-request");

                return null;
            }
        });
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        this.runtime.callInLoader(new Callable() {
            @Override
            public Object call() throws Exception {
                return RingHandler.this.handlerFn.invoke(RingHandler.this.ringFn, exchange);
            }
        });

    }


    private final LoaderWrapper runtime;
    private Object ringFn;
    private final String context;
    private IFn handlerFn;

}
