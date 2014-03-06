package org.projectodd.wunderboss.clojure;

import clojure.java.api.Clojure;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.LoaderWrapper;
import org.projectodd.wunderboss.WunderBoss;

import java.util.concurrent.Callable;

public class ClojureLanguage implements Language {

    @Override
    public void initialize() {
        this.runtime = new LoaderWrapper(WunderBoss.classLoader());

    }

    @Override
    public LoaderWrapper runtime() {
        return this.runtime;
    }

    @Override
    public void shutdown() {
        //TODO: maybe call shutdown-agents here, but only if this will be called once, at actual shutdown, and not once/"app"
    }

    @Override
    public Object eval(final String strToEval) {
        try {
            return this.runtime.callInLoader(new Callable() {
                @Override
                public Object call() throws Exception {
                    return Clojure.var("clojure.core", "eval")
                            .invoke(Clojure.var("clojure.core", "read-string")
                                            .invoke("(do " + strToEval + ")"));
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        return (T) object;
    }

    private LoaderWrapper runtime;
}


