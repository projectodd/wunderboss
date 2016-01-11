/*
 * Copyright 2014-2016 Red Hat, Inc, and individual contributors.
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

package org.projectodd.wunderboss.caching.notifications;

import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryActivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryLoadedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.infinispan.notifications.cachelistener.event.DataRehashedEvent;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.notifications.cachelistener.event.TransactionCompletedEvent;
import org.infinispan.notifications.cachelistener.event.TransactionRegisteredEvent;


public class Listener {

    Listener(Handler handler) {
        this.handler = handler;
    }

    public static Listener listen(Handler handler, String type) {
        switch (type) {
        case "cache_entries_evicted":
            return new CacheEntriesEvicted(handler);
        case "cache_entry_activated":
            return new CacheEntryActivated(handler);
        case "cache_entry_created":
            return new CacheEntryCreated(handler);
        case "cache_entry_invalidated":
            return new CacheEntryInvalidated(handler);
        case "cache_entry_loaded":
            return new CacheEntryLoaded(handler);
        case "cache_entry_modified":
            return new CacheEntryModified(handler);
        case "cache_entry_passivated":
            return new CacheEntryPassivated(handler);
        case "cache_entry_removed":
            return new CacheEntryRemoved(handler);
        case "cache_entry_visited":
            return new CacheEntryVisited(handler);
        case "data_rehashed":
            return new DataRehashed(handler);
        case "topology_changed":
            return new TopologyChanged(handler);
        case "transaction_completed":
            return new TransactionCompleted(handler);
        case "transaction_registered":
            return new TransactionRegistered(handler);
        default:
            throw new IllegalArgumentException("Invalid listener type");
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntriesEvicted extends Listener {
        public CacheEntriesEvicted(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted
        public void handle(CacheEntriesEvictedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntryActivated extends Listener {
        public CacheEntryActivated(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated
        public void handle(CacheEntryActivatedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntryCreated extends Listener {
        public CacheEntryCreated(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated
        public void handle(CacheEntryCreatedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntryInvalidated extends Listener {
        public CacheEntryInvalidated(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated
        public void handle(CacheEntryInvalidatedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntryLoaded extends Listener {
        public CacheEntryLoaded(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntryLoaded
        public void handle(CacheEntryLoadedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntryModified extends Listener {
        public CacheEntryModified(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntryModified
        public void handle(CacheEntryModifiedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntryPassivated extends Listener {
        public CacheEntryPassivated(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated
        public void handle(CacheEntryPassivatedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntryRemoved extends Listener {
        public CacheEntryRemoved(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved
        public void handle(CacheEntryRemovedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class CacheEntryVisited extends Listener {
        public CacheEntryVisited(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited
        public void handle(CacheEntryVisitedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class DataRehashed extends Listener {
        public DataRehashed(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.DataRehashed
        public void handle(DataRehashedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class TopologyChanged extends Listener {
        public TopologyChanged(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.TopologyChanged
        public void handle(TopologyChangedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class TransactionCompleted extends Listener {
        public TransactionCompleted(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.TransactionCompleted
        public void handle(TransactionCompletedEvent e) {
            handler.handle(e);
        }
    }

    @org.infinispan.notifications.Listener
    public static class TransactionRegistered extends Listener {
        public TransactionRegistered(Handler handler) {
            super(handler);
        }
        @org.infinispan.notifications.cachelistener.annotation.TransactionRegistered
        public void handle(TransactionRegisteredEvent e) {
            handler.handle(e);
        }
    }

    protected Handler handler;
}
