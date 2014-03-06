package org.projectodd.wunderboss.scheduling;

import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.ComponentProvider;

public class SchedulingComponentProvider implements ComponentProvider {
    @Override
    public Component newComponent() {
        return new SchedulingComponent();
    }
}
