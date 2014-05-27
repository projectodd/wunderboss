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

package org.projectodd.wunderboss.messaging.hornetq;

import org.projectodd.wunderboss.messaging.Session;

import javax.jms.JMSContext;

public class HornetQSession implements Session {
    public HornetQSession(JMSContext context, Mode mode) {
        this.context = context;
        this.mode = mode;
    }

    @Override
    public Mode mode() {
        return this.mode;
    }

    @Override
    public void commit() {
        this.context.commit();
    }

    @Override
    public void rollback() {
        this.context.rollback();
    }

    @Override
    public void acknowledge() {
        this.context.acknowledge();
    }

    @Override
    public void close() throws Exception {
        this.context.close();
    }

    public JMSContext context() {
        return this.context;
    }

    private final JMSContext context;
    private final Mode mode;
}
