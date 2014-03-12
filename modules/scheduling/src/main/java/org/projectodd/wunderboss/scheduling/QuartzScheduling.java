package org.projectodd.wunderboss.scheduling;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.DirectSchedulerFactory;

import java.util.HashMap;
import java.util.Map;

public class QuartzScheduling implements Scheduling<Scheduler> {

    /*
     options: jobstore? threadpool? other scheduler opts?
     */
    public QuartzScheduling(String name, Options options) {
        this.name = name;
        this.numThreads = options.getInt("num_threads", 5);
    }

    @Override
    public void start() throws Exception {
        System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");
        DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();

        if (!started) {
            factory.createVolatileScheduler(numThreads);
            this.scheduler = factory.getScheduler();
            this.scheduler.start();
            started = true;
            log.info("Quartz started");
        }
    }

    @Override
    public void stop() throws Exception {
        if (started) {
            this.scheduler.shutdown(true);
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

    @Override
    public synchronized boolean schedule(String name, Runnable fn, Map<String, Object> opts) throws Exception {
        Options options = new Options(opts);
        validateOptions(options);

        start();

        boolean replacedExisting = unschedule(name);

        JobDataMap jobDataMap = new JobDataMap();
        // TODO: Quartz says only serializable things should be in here
        jobDataMap.put(RunnableJob.RUN_FUNCTION_KEY, fn);
        JobDetail job = JobBuilder.newJob(RunnableJob.class)
                .usingJobData(jobDataMap)
                .build();

        this.scheduler.scheduleJob(job, initTrigger(name, options));

        this.currentJobs.put("name", job.getKey());

        return replacedExisting;
    }

    @Override
    public synchronized boolean unschedule(String name) throws SchedulerException {
        if (currentJobs.containsKey(name)) {
            this.scheduler.deleteJob(currentJobs.get(name));

            return true;
        }

        return false;
    }

    protected void validateOptions(Options opts) throws IllegalArgumentException {
        if (opts.has(CRON_OPT)) {
            for(String each : new String[] {AT_OPT, EVERY_OPT, IN_OPT, REPEAT_OPT, UNTIL_OPT}) {
                if (opts.has(each)) {
                    throw new IllegalArgumentException("You can't specify both 'cronspec' and '" +
                                                               each + "'");
                }
            }
        }

        if (opts.has(AT_OPT) &&
                opts.has(IN_OPT)) {
            throw new IllegalArgumentException("You can't specify both 'at' and 'in'");
        }

        if (!opts.has(EVERY_OPT)) {
            if (opts.has(REPEAT_OPT)) {
                throw new IllegalArgumentException("You can't specify 'repeat' without 'every'");
            }
            if (opts.has(UNTIL_OPT)) {
                throw new IllegalArgumentException("You can't specify 'until' without 'every'");
            }
        }
    }

    protected Trigger initTrigger(String name, Options opts) {
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(name, name());

        if (opts.has(CRON_OPT)) {
            builder.startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule(opts.getString(CRON_OPT)))
                    .build();
        } else {
            if (opts.has(AT_OPT)) {
                builder.startAt(opts.getDate(AT_OPT));
            } else {
                builder.startNow();
            }

            if (opts.has(UNTIL_OPT)) {
                builder.endAt(opts.getDate(UNTIL_OPT));
            }

            if (opts.has(EVERY_OPT)) {
                SimpleScheduleBuilder schedule =
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInMilliseconds(opts.getInt(EVERY_OPT));
                if (opts.has(REPEAT_OPT)) {
                    schedule.withRepeatCount(opts.getInt(REPEAT_OPT));
                } else {
                    schedule.repeatForever();
                }
                builder.withSchedule(schedule);
            }
        }

        return builder.build();
    }

    private final String name;
    private int numThreads;
    private boolean started;
    private Scheduler scheduler;
    private final Map<String, JobKey> currentJobs = new HashMap<>();

    private static final Logger log = Logger.getLogger(Scheduling.class);
}
