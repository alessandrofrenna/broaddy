/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.alessandrofrenna.broaddy;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DefaultNetworkPeer implements NetworkPeer {
    private final RoutableId<?> peerId;
    private final Map<NetworkId<?>, JoinedNetwork> joinedNetworks = new ConcurrentHashMap<>();

    public DefaultNetworkPeer(RoutableId<?> id) {
        this.peerId = id;
    }

    @Override
    public RoutableId<?> id() {
        return peerId;
    }

    @Override
    public boolean join(BroadcastNetwork network, Consumer<Message<?>> messageConsumer) {
        Objects.requireNonNull(network, "network is required, null provided");
        Objects.requireNonNull(messageConsumer, "message consumer is require, null provided");

        var connect = network.connectPeer(this);
        if (connect == BroadcastNetwork.Connect.OK) {
            joinedNetworks.put(network.id(), new JoinedNetwork(network, messageConsumer));
            return true;
        }
        return false;
    }

    @Override
    public <T> boolean leave(NetworkId<T> networkId) {
        Objects.requireNonNull(networkId,"networkId is required, null provided");
        if (!joinedNetworks.containsKey(networkId)) {
            return false;
        }
        var disconnect = joinedNetworks.get(networkId).network().disconnectPeer(id());
        if (disconnect == BroadcastNetwork.Disconnect.OK) {
            joinedNetworks.remove(networkId);
            return true;
        }
        return false;
    }

    @Override
    public long countJoinedNetworks() {
        return joinedNetworks.size();
    }

    @Override
    public <T, V> void deliverMessage(NetworkId<T> networkId, Message<V> message) {
        if (!joinedNetworks.containsKey(networkId)) {
            return;
        }
        Consumer<Message<?>> genericMessageConsumer = joinedNetworks.get(networkId).consumer();
        try {
            genericMessageConsumer.accept(message);
        } catch (Exception ignore) {
        }
    }

    @Override
    public <T> void forceDisconnection(NetworkId<T> networkId) {
        leave(networkId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DefaultNetworkPeer that = (DefaultNetworkPeer) o;
        return Objects.equals(peerId, that.peerId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(peerId);
    }

    private record JoinedNetwork(BroadcastNetwork network, Consumer<Message<?>> consumer) { }
}
