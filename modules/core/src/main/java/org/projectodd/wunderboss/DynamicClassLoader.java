package org.projectodd.wunderboss;

import java.net.URL;
import java.net.URLClassLoader;

public class DynamicClassLoader extends URLClassLoader {

    public DynamicClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
