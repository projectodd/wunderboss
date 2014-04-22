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

package org.projectodd.wunderboss.messaging.jms;

import org.projectodd.wunderboss.messaging.Endpoint;

import javax.jms.Destination;
import javax.jms.Topic;

public abstract class DestinationEndpoint implements Endpoint<Destination> {

    public DestinationEndpoint(Destination dest, boolean durable) {
        this.destination = dest;
        this.durable = durable;
    }

    @Override
    public boolean isBroadcast() {
        return (this.destination instanceof Topic);
    }

    @Override
    public boolean isDurable() {
        return this.durable;
    }

    @Override
    public Destination implementation() {
        return this.destination;
    }

    private final Destination destination;
    private final boolean durable;
}
