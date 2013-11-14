package org.projectodd.wunderboss.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class RunnableJob implements Job {

    public static final String RUN_FUNCTION_KEY = "run_function";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Runnable runFunction = (Runnable) context.getMergedJobDataMap().get(RUN_FUNCTION_KEY);
        runFunction.run();
    }
}
