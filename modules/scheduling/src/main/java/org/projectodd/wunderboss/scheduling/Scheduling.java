package org.projectodd.wunderboss.scheduling;

import org.projectodd.wunderboss.Component;
import org.quartz.JobKey;
import java.util.Map;

public interface Scheduling<T> extends Component<T> {

    JobKey scheduleJob(String name, Runnable fn, Map<String, Object> options);

    void unscheduleJob(JobKey key);
}
