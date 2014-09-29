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

import com.arjuna.ats.jta.TransactionManager;
import org.jboss.logging.Logger;

import org.projectodd.wunderboss.Options;

public class NarayanaTransaction implements Transaction {

    public NarayanaTransaction(String name, Options options) {
        this.name = name;
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

    public synchronized javax.transaction.TransactionManager manager() {
        if (this.manager == null) {
            this.manager = TransactionManager.transactionManager();
        }
        return this.manager;
    }

    private final String name;
    protected javax.transaction.TransactionManager manager;

    protected static final Logger log = Logger.getLogger(Transaction.class);
}
