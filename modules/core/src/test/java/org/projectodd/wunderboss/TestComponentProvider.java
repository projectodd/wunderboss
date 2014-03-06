package org.projectodd.wunderboss;

public class TestComponentProvider implements ComponentProvider {

    @Override
    public Component create(String name, Options opts) {
        return new TestComponent(name, opts);
    }
}
