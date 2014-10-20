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

import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.messaging.Messaging;

import javax.jms.JMSContext;
import javax.jms.XAJMSContext;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Method;

public class HQXAContext extends HQContext implements Synchronization {
    public HQXAContext(JMSContext jmsContext,
                       Messaging broker,
                       Mode mode,
                       boolean remote) {
        super(jmsContext, broker, mode, remote);
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

    @Override
    public boolean enlist() throws Exception {
        if (tm.getTransaction() == null) {
            return false;
        } else if (!WunderBoss.inContainer() ||
                    isRemote()) {
            XAResource resource = ((XAJMSContext)jmsContext()).getXAResource();
            return tm.getTransaction().enlistResource(resource);
        } else {
            return true;
        }
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            closed = true;
            if (isTransactionActive()) {
                tm.getTransaction().registerSynchronization(this);
            } else {
                super.close();
            }
        }
    }

    @Override
    public void afterCompletion(int status) {
        try {
            super.close();
        } catch (Exception e) {
            throw new RuntimeException("Error after tx complete", e);
        }
    }

    @Override
    public void beforeCompletion() {
        // nothing
    }

    @Override
    public HQSpecificContext createChildContext(Mode mode) {
        throw new IllegalStateException("You can't create a child context from an XA context.");
    }

    @Override
    public boolean isXAEnabled() {
        return true;
    }

    public static boolean isTransactionActive() {
        try {
            return tm != null && tm.getTransaction() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean closed = false;

    public static final TransactionManager tm;
    static {
        TransactionManager found = null;
        try {
            Class clazz = Class.forName("org.projectodd.wunderboss.transactions.Transaction");
            Method method = clazz.getDeclaredMethod("manager");
            Object component = WunderBoss.findOrCreateComponent(clazz);
            found = (TransactionManager) method.invoke(component);
        } catch (Throwable ignored) {}
        tm = found;
    }
}
