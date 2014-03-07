package org.projectodd.wunderboss.scheduling;

import org.projectodd.wunderboss.ComponentProvider;
import org.projectodd.wunderboss.Options;

public class SchedulingProvider implements ComponentProvider<Scheduling> {
    @Override
    public Scheduling create(String name, Options opts) {
        return new QuartzScheduling(name, opts);
    }
}
