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

import org.projectodd.wunderboss.messaging.Connection;

import javax.transaction.TransactionManager;
import javax.jms.JMSContext;

public class HornetQXASession extends HornetQSession {
    public HornetQXASession(Connection connection, JMSContext context, Mode mode, TransactionManager tm) {
        super(connection, context, mode);
        this.tm = tm;
    }

    @Override
    public void commit() {
        // let the TransactionManager handle this
    }

    @Override
    public void rollback() {
        try {
            tm.setRollbackOnly();
        } catch (Exception e) {
            throw new RuntimeException("Error rolling back session", e);
        }
    }

    private TransactionManager tm;
}
