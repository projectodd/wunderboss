package io.wunderboss;

import io.wunderboss.Language;

public class TestLanguage implements Language {
    @Override
    public void initialize(WunderBoss container) {
        registered = true;
    }

    @Override
    public String getRuntime(Options options) {
        return "runtime";
    }

    @Override
    public void destroyRuntime(Object runtime) {
        destroyed = true;
    }

    @Override
    public <T> T coerceToClass(Object object, Class<T> toClass) {
        return (T) object;
    }

    boolean registered;
    boolean destroyed;
}
