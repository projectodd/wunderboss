package org.projectodd.wunderboss.clojure;

import org.projectodd.wunderboss.LoaderWrapper;
import org.projectodd.wunderboss.Language;
import org.projectodd.wunderboss.WunderBoss;

public class ClojureLanguage implements Language {

    @Override
    public void initialize(WunderBoss container) {
        this.container = container;
        this.runtime = new LoaderWrapper(container.classLoader());

    }

    @Override
    public LoaderWrapper runtime() {
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
    private LoaderWrapper runtime;
}


