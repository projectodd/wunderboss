package org.projectodd.wunderboss.clojure;

import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import clojure.api.API;

public class ClojureLanguage implements Language {

    @Override
    public void initialize(WunderBoss container) {

    }

    @Override
    public Object getRuntime(Options options) {
        //use the static runtime for now
        return null;
    }

    @Override
    public void destroyRuntime(Object runtime) {
        clojure.api.API.var("clojure.core", "shutdown-agents").invoke();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        return (T) object;
    }
}
