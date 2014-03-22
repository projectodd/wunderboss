package org.projectodd.wunderboss;

public interface SingletonContext extends Component<Void>, Runnable {

    SingletonContext runnable(Runnable r);
}
