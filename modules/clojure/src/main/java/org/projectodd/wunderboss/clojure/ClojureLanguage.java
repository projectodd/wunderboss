package org.projectodd.wunderboss.clojure;

import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.WunderBoss;

import clojure.java.api.Clojure;

public class ClojureLanguage implements Language {

    @Override
    public void initialize(WunderBoss container) {
        this.container = container;
        this.runtime = new ClojureLoaderWrapper(container.classLoader());

    }

    @Override
    public ClojureLoaderWrapper runtime() {
        return this.runtime;
    }

    @Override
    public void shutdown() {
        //TODO: maybe call shutdown-agents here, but only if this will be called once, at actual shutdown, and not once/"app"
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        return (T) object;
    }

    private WunderBoss container;
    private ClojureLoaderWrapper runtime;
}


