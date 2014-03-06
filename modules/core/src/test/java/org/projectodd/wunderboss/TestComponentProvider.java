package org.projectodd.wunderboss;

public class TestComponentProvider implements ComponentProvider {

    @Override
    public Component newComponent() {
        return new TestComponent();
    }
}
