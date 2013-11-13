package io.wunderboss.job;

import io.wunderboss.Application;
import io.wunderboss.Component;
import io.wunderboss.ComponentInstance;
import io.wunderboss.Options;
import org.jboss.logging.Logger;
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

public class JobComponent extends Component {
    @Override
    public void boot() {
        System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");
        configure(new Options());
        // TODO: Configurable non-lazy boot of Quartz
    }

    @Override
    public void shutdown() {
        if (started) {
            try {
                DirectSchedulerFactory.getInstance().getScheduler().shutdown(true);
            }  catch (SchedulerException e) {
                // TODO: something better
                e.printStackTrace();
            }
            started = false;
            log.info("Quartz stopped");
        }
    }

    @Override
    public void configure(Options options) {
        numThreads = options.getInt("num_threads", 5);
    }

    @Override
    public ComponentInstance start(Application application, Options options) {
        String cronString = options.getString("cron");
        // Cast and retrieve this here to error early if it's not given
        Runnable runFunction = application.coerceObjectToClass(options.get("run_function"), Runnable.class);

        DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
        try {
            if (!started) {
                factory.createVolatileScheduler(numThreads);
                factory.getScheduler().start();
                started = true;
                log.info("Quartz started");
            }

            JobDataMap jobDataMap = new JobDataMap();
            // TODO: Quartz says only serializable things should be in here
            jobDataMap.put(RunnableJob.RUN_FUNCTION_KEY, runFunction);
            JobDetail job = JobBuilder.newJob(RunnableJob.class)
                    .usingJobData(jobDataMap)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronString))
                    .build();

            Scheduler scheduler = factory.getScheduler();
            scheduler.scheduleJob(job, trigger);

            Options instanceOptions = new Options();
            instanceOptions.put("scheduler", scheduler);
            instanceOptions.put("jobKey", job.getKey());
            return new ComponentInstance(this, instanceOptions);
        } catch (SchedulerException e) {
            // TODO: something better
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void stop(ComponentInstance instance) {
        Options options = instance.getOptions();
        Scheduler scheduler = (Scheduler) options.get("scheduler");
        JobKey jobKey = (JobKey) options.get("jobKey");
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            // TODO: something better
            e.printStackTrace();
        }
    }

    private int numThreads;
    private boolean started;

    private static final Logger log = Logger.getLogger(JobComponent.class);
}
