package org.projectodd.wunderboss;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class Application {

    public Application(WunderBoss container, Language language, Options options) {
        this.container = container;
        this.language = language;
        //TODO: verify root *isn't* set here, since it's a container-level opt
        this.options = container.options().merge(options);
    }

    public ComponentInstance start(String componentName) {
        return start(componentName, new Options());
    }

    public ComponentInstance start(String componentName, Map<String, Object> options) {
        return start(componentName, new Options(options));
    }

    public ComponentInstance start(String componentName, Options options) {
        ComponentInstance instance = container.getComponent(componentName).start(this, this.options.merge(options));
        instanceStack.push(instance);
        return instance;
    }

    public void stop() {
        // Stop things in the opposite order that they were added
        Iterator<ComponentInstance> iterator = instanceStack.iterator();
        while (iterator.hasNext()) {
            ComponentInstance instance = iterator.next();
            if (instance != null) {
                instance.stop();
            }
            iterator.remove();
        }
    }

    public Object runtime() {
        return language.runtime();
    }

    public <T> T coerceObjectToClass(Object object, Class<T> toClass) {
        return language.coerceToClass(object, toClass);
    }

    private final WunderBoss container;
    private final Language language;
    private final Options options;
    private final Deque<ComponentInstance> instanceStack = new LinkedList<>();
}
