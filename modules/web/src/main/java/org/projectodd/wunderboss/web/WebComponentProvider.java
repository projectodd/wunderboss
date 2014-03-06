package org.projectodd.wunderboss.web;

import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.ComponentProvider;

public class WebComponentProvider implements ComponentProvider {

    @Override
    public Component newComponent() {
        return new WebComponent();
    }
}

