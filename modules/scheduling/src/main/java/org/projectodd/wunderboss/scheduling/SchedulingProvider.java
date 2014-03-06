package org.projectodd.wunderboss.scheduling;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;

public class SchedulingProvider implements ComponentProvider<SchedulingComponent> {
    @Override
    public SchedulingComponent create(String name, Options opts) {
        return new Scheduling(name, opts);
    }
}
