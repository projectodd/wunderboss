/*
 * Copyright 2014 Red Hat, Inc, and individual contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectodd.wunderboss.scheduling;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.SingletonContext;
import org.projectodd.wunderboss.WunderBoss;
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

import static org.projectodd.wunderboss.scheduling.Scheduling.ScheduleOption.*;

public class QuartzScheduling implements Scheduling<Scheduler> {

    /*
     options: jobstore? threadpool? other scheduler opts?
     */
    public QuartzScheduling(String name, Options<CreateOption> options) {
        this.name = name;
        this.numThreads = options.getInt(CreateOption.NUM_THREADS, 5);
    }

    @Override
    public void start() throws Exception {
        if (!started) {
            System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");
            DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();

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
    public synchronized boolean schedule(String name, Runnable fn, Map<ScheduleOption, Object> opts) throws Exception {
        Options<ScheduleOption> options = new Options<>(opts);
        validateOptions(options);

        start();

        boolean replacedExisting = unschedule(name);

        JobDataMap jobDataMap = new JobDataMap();

        if (options.getBoolean(SINGLETON, false)) {
            fn = WunderBoss.findOrCreateComponent(SingletonContext.class, name, null).runnable(fn);
        }

        // TODO: Quartz says only serializable things should be in here
        jobDataMap.put(RunnableJob.RUN_FUNCTION_KEY, fn);
        JobDetail job = JobBuilder.newJob(RunnableJob.class)
                .usingJobData(jobDataMap)
                .build();

        this.scheduler.scheduleJob(job, initTrigger(name, options));

        this.currentJobs.put(name, job.getKey());

        return replacedExisting;
    }

    @Override
    public synchronized boolean unschedule(String name) throws SchedulerException {
        if (currentJobs.containsKey(name)) {
            this.scheduler.deleteJob(currentJobs.remove(name));

            return true;
        }

        return false;
    }

    protected void validateOptions(Options<ScheduleOption> opts) throws IllegalArgumentException {
        if (opts.has(CRON)) {
            for(ScheduleOption each : new ScheduleOption[] {EVERY, LIMIT}) {
                if (opts.has(each)) {
                    throw new IllegalArgumentException("You can't specify both 'cron' and '" +
                                                               each.value + "'");
                }
            }
        }

        if (opts.has(AT) &&
                opts.has(IN)) {
            throw new IllegalArgumentException("You can't specify both 'at' and 'in'");
        }

        if (!opts.has(EVERY)) {
            if (opts.has(LIMIT)) {
                throw new IllegalArgumentException("You can't specify 'limit' without 'every'");
            }
            if (opts.has(UNTIL) &&
                    !opts.has(CRON)) {
                throw new IllegalArgumentException("You can't specify 'until' without 'every' or 'cron'");
            }
        }
    }

    protected Trigger initTrigger(String name, Options<ScheduleOption> opts) {
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(name, name());

        if (opts.has(AT)) {
            builder.startAt(opts.getDate(AT));
        } else if (opts.has(IN)) {
            builder.startAt(new java.util.Date(System.currentTimeMillis() + opts.getLong(IN)));
        } else {
            builder.startNow();
        }

        if (opts.has(UNTIL)) {
            builder.endAt(opts.getDate(UNTIL));
        }

        if (opts.has(CRON)) {
            builder.withSchedule(CronScheduleBuilder.cronSchedule(opts.getString(CRON)));
        } else if (opts.has(EVERY)) {
            SimpleScheduleBuilder schedule =
                    SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(opts.getInt(EVERY));
            if (opts.has(LIMIT)) {
                schedule.withRepeatCount(opts.getInt(LIMIT) - 1);
            } else {
                schedule.repeatForever();
            }
            builder.withSchedule(schedule);
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
