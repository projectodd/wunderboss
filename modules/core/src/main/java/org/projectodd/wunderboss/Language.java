package org.projectodd.wunderboss;

public interface Language {

    public void initialize(WunderBoss container);

    public Object runtime();

    public void shutdown();

    public Object eval(String toEval);

    public <T> T coerceToClass(Object object, Class<T> toClass);
}
