package org.projectodd.wunderboss;

public interface ComponentProvider<T extends Component> {
    public T create(String name, Options options);
}
