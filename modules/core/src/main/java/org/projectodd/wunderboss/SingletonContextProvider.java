package org.projectodd.wunderboss;

/**
 * Created by tcrawley on 3/20/14.
 */
public class SingletonContextProvider implements ComponentProvider<SingletonContext> {

    @Override
    public SingletonContext create(String name, Options options) {
        System.out.println("TC: WRONG PROVIDER");
        return new AlwaysRunContext(name);
    }
}
