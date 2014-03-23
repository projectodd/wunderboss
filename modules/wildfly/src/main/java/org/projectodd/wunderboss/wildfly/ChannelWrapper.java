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

package org.projectodd.wunderboss.wildfly;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.locking.LockService;
import org.jgroups.util.RspList;
import org.projectodd.wunderboss.Component;
import org.projectodd.wunderboss.WunderBoss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class ChannelWrapper  extends ReceiverAdapter implements RequestHandler, Component<Channel> {
    public ChannelWrapper(String name) {
        this.name = name;
    }

    @Override
    public void start() throws Exception {
        if (this.channel == null) {
            this.channel = ClusterUtils.lockableChannel(this.name);
            this.dispatcher = new MessageDispatcher(this.channel, null, this, this);
            this.lockService = new LockService(this.channel);
            this.channel.connect(WunderBoss.options().getString("deployment-name"));
        }
    }

    @Override
    public void stop() throws Exception {
        if (this.channel != null) {
            this.channel.disconnect();
            this.channel.close();
            this.channel = null;
        }
    }

    @Override
    public Channel implementation() {
        if (this.channel == null) {
            try {
                start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.channel;
    }

    @Override
    public String name() {
        return this.name;
    }

    public Lock getLock(String name) {
        return this.lockService.getLock(name);
    }

    public ChannelWrapper registerHandler(String id, RequestHandler handler) {
        registeredHandlers.put(id, handler);

        return this;
    }

    public ChannelWrapper addMembershipListener(MembershipListener listener) {
        this.membershipListeners.add(listener);

        return this;
    }

     public RspList<Address> castMessage(List<Address> dests, Object msg, long timeout) throws Exception {
         return this.dispatcher.castMessage(dests, new Message(null, msg),
                                            new RequestOptions(ResponseMode.GET_ALL, timeout));
    }

    @Override
    public void viewAccepted(View view) {
        for(MembershipListener each : membershipListeners) {
            each.viewAccepted(view);;
        }
    }

    @Override
    public Object handle(Message msg) throws Exception {
        Map content = (Map)msg.getObject();
        if (this.registeredHandlers.containsKey(content.get("dispatch"))) {
            return this.registeredHandlers.get(content.get("dispatch")).handle(msg);
        }

        return null;
    }

    private final String name;
    private JChannel channel;
    private MessageDispatcher dispatcher;
    private LockService lockService;
    private final Map<String, RequestHandler> registeredHandlers = new HashMap<>();
    private final List<MembershipListener> membershipListeners = new ArrayList<>();
}

