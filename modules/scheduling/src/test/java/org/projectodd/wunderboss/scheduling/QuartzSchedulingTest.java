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
                public void run() {
                    latch.countDown();
                }},
            options);
        assert(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testAtStyleWorks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        options.put(EVERY, 100);
        options.put(REPEAT, 1);

        scheduler.schedule("attie", new Runnable() {
                public void run() {
                    latch.countDown();
                }},
            options);
        assert(latch.await(5, TimeUnit.SECONDS));
    }
}
