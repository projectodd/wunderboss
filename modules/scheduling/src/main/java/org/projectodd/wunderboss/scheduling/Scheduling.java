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

import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.Option;

import java.util.Map;

public interface Scheduling extends Component {
    class CreateOption extends Option {
        public static final CreateOption NUM_THREADS = opt("num_threads", 5, CreateOption.class);
    }

    class ScheduleOption extends Option {
        public static final ScheduleOption CRON      = opt("cron", ScheduleOption.class);
        public static final ScheduleOption AT        = opt("at", ScheduleOption.class);
        public static final ScheduleOption EVERY     = opt("every", ScheduleOption.class);
        public static final ScheduleOption IN        = opt("in", ScheduleOption.class);
        public static final ScheduleOption LIMIT     = opt("limit", ScheduleOption.class);
        public static final ScheduleOption UNTIL     = opt("until", ScheduleOption.class);
        public static final ScheduleOption SINGLETON = opt("singleton", ScheduleOption.class);
    }

    /**
     *
     * @param name
     * @param lambda
     * @param options cron - String
     *                at - java.util.Date
     *                every - ms
     *                in - ms
     *                limit - int
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
