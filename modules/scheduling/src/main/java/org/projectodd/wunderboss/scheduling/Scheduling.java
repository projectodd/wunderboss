package org.projectodd.wunderboss.scheduling;

import org.projectodd.wunderboss.Component;
import java.util.Map;

public interface Scheduling<T> extends Component<T> {
    enum CreateOption {
        NUM_THREADS("num_threads");

        CreateOption(String value) {
            this.value = value;
        }

        public String value;
    }

    enum ScheduleOption {
        CRON("cronspec"),
        AT("at"),
        EVERY("every"),
        IN("in"),
        REPEAT("repeat"),
        UNTIL("until");

        ScheduleOption(String value) {
            this.value = value;
        }

        public String value;
    }

    /**
     *
     * @param name
     * @param lambda
     * @param options cronspec - String
     *                at - java.util.Date
     *                every - ms
     *                in - ms
     *                repeat - int
     *                until - java.util.Date
     *                singleton
     * @return true if schedule replaced a job with the same name, throws on failure
     */
    boolean schedule(String name, Runnable lambda, Map<ScheduleOption, Object> options) throws Exception;

    /**
     *
     * @param name
     * @return true if the job exists
     */
    boolean unschedule(String name) throws Exception;


}
