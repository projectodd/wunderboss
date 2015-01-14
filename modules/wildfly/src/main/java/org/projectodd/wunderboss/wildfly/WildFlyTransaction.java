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

package org.projectodd.wunderboss.wildfly;

import org.jboss.msc.service.ServiceRegistry;
import org.projectodd.wunderboss.Options;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.transactions.NarayanaTransaction;

import javax.transaction.TransactionManager;


public class WildFlyTransaction extends NarayanaTransaction {

    public WildFlyTransaction(String name, Options options) {
        super(name, options);
    }

    public synchronized TransactionManager manager() {
        if (this.manager == null) {
            this.manager = getWildFlyTransactionManager();
        }
        return this.manager;
    }

    private TransactionManager getWildFlyTransactionManager() {
        ServiceRegistry serviceRegistry = (ServiceRegistry) WunderBoss.options().get("service-registry");
        return (TransactionManager) serviceRegistry.getRequiredService(WildFlyService.TRANSACTION_MANAGER).getValue();
    }
}
