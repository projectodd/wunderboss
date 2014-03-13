package org.projectodd.wunderboss.scheduling;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.projectodd.wunderboss.WunderBoss;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.projectodd.wunderboss.scheduling.Scheduling.ScheduleOption.*;

public class QuartzSchedulingTest {
    Scheduling scheduler;
    Map <Scheduling.ScheduleOption, Object> options;

    //TODO: test: unschedule, validations, all opt permutations?

    @Before
    public void setup() throws Exception {
        scheduler = WunderBoss.findOrCreateComponent(Scheduling.class);
        options = new HashMap<>();
    }

    @After
    public void teardown() throws Exception {
        scheduler.stop();
    }

    @Test
    public void testCronSpecWorks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        options.put(CRON, "* * * * * ?");

        scheduler.schedule("cronnie", new Runnable() {
                               @Override
                               public void run() {
                                   latch.countDown();
                               }
                           },
                           options);
        assert(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testAtStyleWorks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        options.put(EVERY, 100);
        options.put(REPEAT, 1);

        scheduler.schedule("attie", new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        },
                           options);
        assert(latch.await(5, TimeUnit.SECONDS));
    }
}
