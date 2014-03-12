package org.projectodd.wunderboss.scheduling;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.projectodd.wunderboss.WunderBoss;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class QuartzSchedulingTest {
    QuartzScheduling scheduler;
    Map <String, Object> options;

    //TODO: test validations, all opt permutations?

    @Before
    public void setup() throws Exception {
        scheduler = (QuartzScheduling)WunderBoss.findOrCreateComponent("scheduling");
        options = new HashMap<>();
    }

    @After
    public void teardown() throws Exception {
        scheduler.stop();
    }

    @Test
    public void testCronSpecWorks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        options.put(Scheduling.CRON_OPT, "* * * * * ?");

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

        options.put(Scheduling.EVERY_OPT, 100);
        options.put(Scheduling.REPEAT_OPT, 1);

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
