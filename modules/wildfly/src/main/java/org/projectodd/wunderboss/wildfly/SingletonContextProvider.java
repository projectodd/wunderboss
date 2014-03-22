package org.projectodd.wunderboss.wildfly;

import org.projectodd.wunderboss.AlwaysRunContext;
import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.SingletonContext;

public class SingletonContextProvider implements ComponentProvider<SingletonContext> {

    @Override
    public SingletonContext create(String name, Options options) {
        System.out.println("TC: RIGHT PROVIDER");
        if (ClusterUtils.inCluster()) {
            System.out.println("IN CLUSTER");
        return new ClusteredSingletonContext(name);
        } else {
            System.out.println("NOT IN CLUSTER");
            return new AlwaysRunContext(name);
        }
    }
}
