package org.projectodd.wunderboss;

public class TestLanguage implements Language {
    @Override
    public void initialize(WunderBoss container) {
        registered = true;
    }

    @Override
    public String runtime() {
        return "runtime";
    }

    @Override
    public void shutdown() {
        destroyed = true;
    }

    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        return (T) object;
    }

    boolean registered;
    boolean destroyed;
}
