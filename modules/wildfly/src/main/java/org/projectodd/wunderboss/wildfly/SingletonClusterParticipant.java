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

import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.projectodd.wunderboss.WunderBoss;
import org.projectodd.wunderboss.ec.ClusterChangeCallback;
import org.projectodd.wunderboss.ec.ClusterParticipant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

public class SingletonClusterParticipant implements ClusterParticipant, RequestHandler, MembershipListener {
    public SingletonClusterParticipant(String name) {
        this.name = name;
        connect();
    }

    @Override
    public synchronized void connect() {
        if (this.channelWrapper == null) {
            this.channelWrapper = WunderBoss.findOrCreateComponent(ChannelWrapper.class, this.name, null);
            try {
                this.channelWrapper.start();
            } catch (Exception e) {
                throw new RuntimeException("Failed to start JGroups channel", e);
            }
            this.channelWrapper.registerHandler(name, this).addMembershipListener(this);
        }
    }

    @Override
    public synchronized void disconnect() {
        if (this.channelWrapper != null) {
            try {
                this.channelWrapper.stop();
            } catch (Exception e) {
                throw new RuntimeException("Failed to stop JGroups channel", e);
            }
            this.channelWrapper = null;
            this.currentMaster = null;
        }
    }

    @Override
    public void viewAccepted(View view) {
        if (!view.containsMember(this.currentMaster)) {
            //TODO: synchronize access to currentMaster?
            // clear the master, since it no longer exists in the cluster
            this.currentMaster = null;
        }

        if (this.clusterChangeCallback != null) {
            this.clusterChangeCallback.clusterChanged(isMasterWithoutInterrogatingCluster(), isMaster());
        }
    }

    @Override
    public void suspect(Address suspected_mbr) {

    }

    @Override
    public void block() {

    }

    @Override
    public void unblock() {

    }

    @Override
    public Object handle(Message msg) {
        Map content = (Map)msg.getObject();
        Address requestedMaster = (Address)content.get("payload");
        Address oldMaster = this.currentMaster;
        if (this.currentMaster == null) {
            this.currentMaster = requestedMaster;
        }

        return oldMaster;
    }

    public Address id() {
        return this.channelWrapper.channel().getAddress();
    }

    public boolean isMasterWithoutInterrogatingCluster() {
        final Address id = id();

        return id != null &&
                id.equals(this.currentMaster);
    }

    @Override
    public void setClusterChangeCallback(ClusterChangeCallback callback) {
        this.clusterChangeCallback = callback;
    }

    /*
     * - if master is known
     *     - run if we are it
     * - else
     *   - lock
     *     - check master again, run if master
     *     - tell other nodes we are master
     *     - wait for responses
     *     - responses are the id each node thought was master, which will be null if we are to be master
     *     - if all are null, we are master
     *     - else if all agree someone else is master, store that id
     *     - else throw
     *   - unlock
     */
    @Override
    public boolean isMaster() {
        boolean isMaster = false;

        try {
            if (this.currentMaster != null) {
                isMaster = isMasterWithoutInterrogatingCluster();
            } else {
                Lock lock = this.channelWrapper.getLock(this.name);
                lock.lock();
                try {
                    if (this.currentMaster != null) {
                        //I don't think we can become master while someone else holds the lock, but...
                        isMaster = isMasterWithoutInterrogatingCluster();
                    } else if (id() != null) { // id() will be null if jgroups is shut/shutting down
                        Map<String, Object> msg = new HashMap();
                        msg.put("dispatch", this.name);
                        msg.put("payload", id());

                        //TODO: what happens on timeout?
                        RspList<Address> responses =
                                this.channelWrapper.castMessage(null, msg, 5000);

                        Set<Address> possibleMasters = new HashSet<>();
                        Address reportedMaster = null;
                        for(Map.Entry<Address, Rsp<Address>> each : responses.entrySet()) {
                            Address addr = each.getValue().getValue();
                            if (addr != null) {
                                possibleMasters.add(addr);
                                reportedMaster = addr;
                            }
                        }

                        if (possibleMasters.size() > 1) {
                            throw new IllegalStateException("Cluster in broken state, multiple masters for " + this.name +
                                                                    ": " + possibleMasters);
                        }

                        if (reportedMaster == null) {
                            isMaster = true;
                        } else {
                            this.currentMaster = reportedMaster;
                            isMaster = false;
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isMaster;
    }

    private final String name;
    private ChannelWrapper channelWrapper;
    private Address currentMaster = null;
    private ClusterChangeCallback clusterChangeCallback;

    private static final Logger log = Logger.getLogger(SingletonClusterParticipant.class);
}
