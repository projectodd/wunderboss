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

package org.projectodd.wunderboss.transactions;

import org.jboss.logging.Logger;
import org.projectodd.wunderboss.Options;

import javax.transaction.TransactionManager;
import java.util.concurrent.Callable;

public class NarayanaTransaction implements Transaction {

    public NarayanaTransaction(String name, Options options) {
        this.name = name;
    }

    @Override
    public Object required(Callable f) throws Exception {
        return isActive() ? f.call() : begin(f);
    }

    @Override
    public Object requiresNew(final Callable f) throws Exception {
        return isActive() ?
            suspend(new Callable() {
                    public Object call() throws Exception {
                        return begin(f);
                    }
                })
            : begin(f);
    }

    @Override
    public Object notSupported(Callable f) throws Exception {
        return isActive() ? suspend(f) : f.call();
    }

    @Override
    public Object supports(Callable f) throws Exception {
        return f.call();
    }

    @Override
    public Object mandatory(Callable f) throws Exception {
        if (isActive()) {
            return f.call();
        } else {
            throw new Exception("No active transaction");
        }
    }

    @Override
    public Object never(Callable f) throws Exception {
        if (isActive()) {
            throw new Exception("Active transaction detected");
        } else {
            return f.call();
        }
    }

    @Override
    public void start() throws Exception {
        manager();
    }

    @Override
    public synchronized void stop() throws Exception {
        this.manager = null;
    }

    @Override
    public boolean isRunning() {
        return this.manager != null;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public synchronized TransactionManager manager() {
        if (this.manager == null) {
            this.manager = com.arjuna.ats.jta.TransactionManager.transactionManager();
        }
        return this.manager;
    }

    public boolean isActive() throws Exception {
        return null != current();
    }

    public javax.transaction.Transaction current() throws Exception {
        return manager().getTransaction();
    }

    Object begin(Callable f) throws Exception {
        TransactionManager mgr = manager();
        mgr.begin();
        try {
            Object result = f.call();
            mgr.commit();
            return result;
        } catch (javax.transaction.RollbackException ignored) {
            return null;
        } catch (Throwable e) {
            log.warn("Exception occurred during transaction; rolling back", e);
            mgr.rollback();
            throw new RuntimeException("Transaction rolled back", e);
        }
    }

    Object suspend(Callable f) throws Exception {
        TransactionManager mgr = manager();
        javax.transaction.Transaction tx = mgr.suspend();
        try {
            return f.call();
        } finally {
            mgr.resume(tx);
        }
    }

    private final String name;
    protected TransactionManager manager;

    protected static final Logger log = Logger.getLogger(Transaction.class);
}
