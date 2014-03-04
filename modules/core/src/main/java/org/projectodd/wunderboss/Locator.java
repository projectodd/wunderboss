package org.projectodd.wunderboss;

public interface Locator {
    public void setClassLoader(ClassLoader loader);
    public Language findLanguage(String name);
    public Component findComponent(String name);
}
