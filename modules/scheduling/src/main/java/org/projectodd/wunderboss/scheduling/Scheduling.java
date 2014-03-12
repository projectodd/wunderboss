package org.projectodd.wunderboss.scheduling;

import org.projectodd.wunderboss.Component;
import java.util.Map;

public interface Scheduling<T> extends Component<T> {
    static final String CRON_OPT = "cronspec";
    static final String AT_OPT = "at";
    static final String EVERY_OPT = "every";
    static final String IN_OPT = "in";
    static final String REPEAT_OPT = "repeat";
    static final String UNTIL_OPT = "until";

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
    boolean schedule(String name, Runnable lambda, Map<String, Object> options) throws Exception;

    /**
     *
     * @param name
     * @return true if the job exists
     */
    boolean unschedule(String name) throws Exception;


}
