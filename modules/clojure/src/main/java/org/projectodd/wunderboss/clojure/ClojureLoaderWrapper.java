package org.projectodd.wunderboss.clojure;

import java.util.concurrent.Callable;

public class ClojureLoaderWrapper {

    public ClojureLoaderWrapper(ClassLoader loader) {
        this.loader = loader;
    }

    public Object callInLoader(Callable body) {
        Thread thread = Thread.currentThread();
        ClassLoader oldCL = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(this.loader);

            return body.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            thread.setContextClassLoader(oldCL);
        }

    }

    private final ClassLoader loader;
}
