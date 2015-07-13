/*
 * Copyright 2014-2015 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.messaging.jms;

import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.messaging.Messaging;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XAConnection;
import javax.jms.XASession;
import javax.transaction.Synchronization;
import javax.transaction.xa.XAResource;

public class JMSXAContext extends JMSContext implements Synchronization {
    public JMSXAContext(Connection jmsConnection,
                        Messaging broker,
                        Mode mode,
                        boolean remote) {
        super(jmsConnection, broker, mode, remote, null);
    }

    @Override
    public void commit() {
        // let the TransactionManager handle this
    }

    @Override
    public void rollback() {
        try {
            TransactionUtil.tm.setRollbackOnly();
        } catch (Exception e) {
            throw new RuntimeException("Error rolling back session", e);
        }
    }

    @Override
    public boolean enlist() throws Exception {
        if (TransactionUtil.tm.getTransaction() == null) {
            return super.isXAEnabled();
        } else if (!WunderBoss.inContainer() ||
                    isRemote()) {
            XAResource resource = ((XASession)jmsSession()).getXAResource();
            return TransactionUtil.tm.getTransaction().enlistResource(resource);
        } else {
            return true;
        }
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            closed = true;
            if (TransactionUtil.isTransactionActive()) {
                TransactionUtil.tm.getTransaction().registerSynchronization(this);
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
    public JMSSpecificContext createChildContext(Mode mode) {
        throw new IllegalStateException("You can't create a child context from an XA context.");
    }

    @Override
    public boolean isXAEnabled() {
        return true;
    }

    @Override
    protected Session createJMSSession() throws JMSException {
        return ((XAConnection)jmsConnection()).createXASession();
    }

    private boolean closed = super.isXAEnabled();
}
