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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultNetworkPeer implements NetworkPeer {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultNetworkPeer.class);

    private final RoutableId<?> peerId;
    private final Map<NetworkId<?>, JoinedNetwork> joinedNetworks = new ConcurrentHashMap<>();

    public DefaultNetworkPeer(RoutableId<?> peerId) {
        if (Objects.isNull(peerId)) {
            throw new IllegalArgumentException("peerId is required, null provided");
        }
        this.peerId = peerId;
        LOG.info("Created NetworkPeer with id {}", peerId);
    }

    @Override
    public RoutableId<?> id() {
        return peerId;
    }

    @Override
    public boolean join(BroadcastNetwork network, Consumer<Message<?>> messageConsumer) {
        if (Objects.isNull(network)) {
            throw new IllegalArgumentException("network is required, null provided");
        }
        if (Objects.isNull(messageConsumer)) {
            throw new IllegalArgumentException("messageConsumer is required, null provided");
        }

        final NetworkId<?> networkId = network.id();
        LOG.trace("Joining NetworkPeer {} to BroadcastNetwork with id {}", peerId, networkId);
        var connect = network.connectPeer(this);
        if (connect == BroadcastNetwork.Connect.OK) {
            joinedNetworks.put(network.id(), new JoinedNetwork(network, messageConsumer));
            LOG.info("NetworkPeer {} joined to BroadcastNetwork with id {}: {}", peerId, networkId, connect.description());
            return true;
        }
        LOG.error("NetworkPeer {} join to BroadcastNetwork with id {} failed: {}", peerId, networkId, connect.description());
        return false;
    }

    @Override
    public boolean leave(NetworkId<?> networkId) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }

        if (!joinedNetworks.containsKey(networkId)) {
            LOG.info("NetworkPeer {} has not joined a BroadcastNetwork with id {}", peerId, networkId);
            return false;
        }

        LOG.trace("NetworkPeer {} is leaving BroadcastNetwork with id {}", peerId, networkId);
        var disconnect = joinedNetworks.get(networkId).network().disconnectPeer(id());
        if (disconnect == BroadcastNetwork.Disconnect.OK) {
            joinedNetworks.remove(networkId);
            LOG.info("NetworkPeer {} left BroadcastNetwork with id {}: {}", peerId, networkId, disconnect.description());
            return true;
        }
        LOG.error("NetworkPeer {} failed to leave BroadcastNetwork with id {}: {}", peerId, networkId, disconnect.description());
        return false;
    }

    @Override
    public long countJoinedNetworks() {
        return joinedNetworks.size();
    }

    @Override
    public void deliverMessage(NetworkId<?> networkId, Message<?> message) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }

        if (!joinedNetworks.containsKey(networkId)) {
            return;
        }
        Consumer<Message<?>> genericMessageConsumer = joinedNetworks.get(networkId).consumer();
        try {
            LOG.info("NetworkPeer {} received a message from BroadcastNetwork with id {}", peerId, networkId);
            genericMessageConsumer.accept(message);
        } catch (Exception e) {
            LOG.error("Error processing the message payload: {}", e.getMessage(), e);
        }
    }

    @Override
    public void forceDisconnection(NetworkId<?> networkId) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }

        LOG.info("NetworkPeer {} was forced to leave BroadcastNetwork with id {}", id(), networkId);
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
