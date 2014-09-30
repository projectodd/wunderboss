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
import org.projectodd.wunderboss.messaging.Connection;
import org.projectodd.wunderboss.messaging.Session;

import javax.jms.JMSContext;
import javax.jms.XAJMSContext;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import java.lang.reflect.Method;

public class Sessionator {

    public HornetQSession create(Connection c, JMSContext s, Session.Mode m) throws Exception {
        if (s.getTransacted() && tm != null && tm.getTransaction() != null) {
            XAResource xar = ((XAJMSContext) s).getXAResource();
            tm.getTransaction().enlistResource(xar);
            return new HornetQXASession(c, s, m, tm);
        } else {
            return new HornetQSession(c, s, m);
        }
    }

    private static final TransactionManager tm;
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
