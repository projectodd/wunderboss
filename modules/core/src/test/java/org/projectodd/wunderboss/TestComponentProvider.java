package org.projectodd.wunderboss;

public class TestComponentProvider implements ComponentProvider<TestComponent> {

    @Override
    public TestComponent create(String name, Options opts) {
        return new TestComponent(name, opts);
    }
}
