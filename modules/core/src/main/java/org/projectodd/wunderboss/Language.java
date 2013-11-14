package org.projectodd.wunderboss;

public interface Language {

    public void initialize(WunderBoss container);

    public Object getRuntime(Options options);

    public void destroyRuntime(Object runtime);

    public <T> T coerceToClass(Object object, Class<T> toClass);
}
