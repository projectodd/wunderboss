package org.projectodd.wunderboss;

public interface Locator {
    public Language findLanguage(String name);
    public ComponentProvider findComponentProvider(Class<? extends Component> clazz);
}
