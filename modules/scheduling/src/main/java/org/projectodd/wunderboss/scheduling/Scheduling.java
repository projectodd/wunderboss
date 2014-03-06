package org.projectodd.wunderboss.scheduling;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Options;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;

public class Scheduling implements Component<Scheduler> {
    public Scheduling(String name, Options options) {
        this.name = name;
        this.numThreads = options.getInt("num_threads", 5);
    }

    @Override
    public void start() {
        System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");
        // TODO: Configurable non-lazy boot of Quartz
    }

    @Override
    public void stop() {
        if (started) {
            try {
               this.scheduler.shutdown(true);
            }  catch (SchedulerException e) {
                // TODO: something better
                e.printStackTrace();
            }
            started = false;
            log.info("Quartz stopped");
        }
    }

    @Override
    public Scheduler implementation() {
        return this.scheduler;
    }

    @Override
    public String name() {
        return this.name;
    }

    // options:
    // cron, run_function (takes a Map), data (the Map), at options?
    public JobKey scheduleJob(String name, Runnable fn, Options options) {
        String cronString = options.getString("cron");
        // Cast and retrieve this here to error early if it's not given
        DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
        try {
            if (!started) {
                factory.createVolatileScheduler(numThreads);
                this.scheduler = factory.getScheduler();
                this.scheduler.start();
                started = true;
                log.info("Quartz started");
            }

            JobDataMap jobDataMap = new JobDataMap();
            // TODO: Quartz says only serializable things should be in here
            jobDataMap.put(RunnableJob.RUN_FUNCTION_KEY, fn);
            JobDetail job = JobBuilder.newJob(RunnableJob.class)
                    .usingJobData(jobDataMap)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronString))
                    .build();

            this.scheduler.scheduleJob(job, trigger);

            Options instanceOptions = new Options();
            instanceOptions.put("scheduler", scheduler);
            return job.getKey();
        } catch (SchedulerException e) {
            // TODO: something better
            e.printStackTrace();
            return null;
        }
    }

    public void unscheduleJob(JobKey key) {
        try {
            this.scheduler.deleteJob(key);
        } catch (SchedulerException e) {
            // TODO: something better
            e.printStackTrace();
        }
    }

    private final String name;
    private int numThreads;
    private boolean started;
    private Scheduler scheduler;

    private static final Logger log = Logger.getLogger(Scheduling.class);
}
