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

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultBroadcastNetwork implements BroadcastNetwork {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultBroadcastNetwork.class);
    private final NetworkId<?> networkId;
    private final Set<Routable> routablePeer = new CopyOnWriteArraySet<>();
    private final Lock networkLock = new ReentrantLock(true);

    private volatile Status networkStatus;
    private CompletableFuture<Void> shutdownCompletionFuture;

    public DefaultBroadcastNetwork(NetworkId<?> networkId) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }
        this.networkId = networkId;
        this.networkStatus = Status.ONLINE;
        LOG.info("Created a BroadcastNetwork with id {}", networkId);
        LOG.trace("BroadcastNetwork {} - Online - Ready to accept peers", networkId);
    }

    @Override
    public NetworkId<?> id() {
        return networkId;
    }

    @Override
    public Connect connectPeer(Routable peer) {
        if (Objects.isNull(peer)) {
            throw new IllegalArgumentException("peer is required, null provided");
        }

        networkLock.lock();
        try {
            if (networkStatus != Status.ONLINE) { // Combined check
                return networkStatus == Status.SHUTTING_DOWN ? Connect.NETWORK_SHUTTING_DOWN : Connect.NETWORK_OFFLINE;
            }
            if (routablePeer.contains(peer)) {
                return Connect.EXISTING_ID;
            }
            if (routablePeer.add(peer)) {
                return Connect.OK;
            }
            return Connect.FAILED; // Should ideally not be reached with COWSet if contains was false
        } finally {
            networkLock.unlock();
        }
    }

    @Override
    public <U> Disconnect disconnectPeer(RoutableId<U> routableId) {
        if (Objects.isNull(routableId)) {
            throw new IllegalArgumentException("routableId is required, null provided");
        }

        if (routablePeer.removeIf(networkPeer -> networkPeer.id().equals(routableId))) {
            networkLock.lock();
            try {
                if (networkStatus == Status.SHUTTING_DOWN && routablePeer.isEmpty() && shutdownCompletionFuture != null && !shutdownCompletionFuture.isDone()) {
                    networkStatus = Status.OFFLINE;
                    shutdownCompletionFuture.complete(null);
                }
            } finally {
                networkLock.unlock();
            }
            return Disconnect.OK;
        }
        return Disconnect.NOT_FOUND;
    }

    @Override
    public long size() {
        return routablePeer.size();
    }

    @Override
    public boolean isEmpty() {
        return routablePeer.isEmpty();
    }

    @Override
    public <T>  void broadcast(Message<T> message) {
        networkLock.lock();
        try {
            if (networkStatus == Status.OFFLINE || routablePeer.isEmpty()) {
                return;
            }
        } finally {
            networkLock.unlock();
        }
        // If we reach here, status is either Online or ShuttingDown, and peers exist.
        // The next forEach should operate on a snapshot of the networkPeers COW array set
        routablePeer.forEach(networkPeer -> networkPeer.deliverMessage(id(), message));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        networkLock.lock();
        try {
            if (networkStatus == Status.SHUTTING_DOWN) {
                LOG.trace("BroadcastNetwork {} - Shutting down - No more connectPeer requests will be accepted", networkId);
                return shutdownCompletionFuture;
            }
            if (networkStatus == Status.OFFLINE) {
                LOG.trace("BroadcastNetwork {} - Offline - Ready to be removed", networkId);
                return CompletableFuture.completedFuture(null);
            }

            shutdownCompletionFuture = new CompletableFuture<>();
            if (routablePeer.isEmpty()) {
                networkStatus = Status.OFFLINE;
                shutdownCompletionFuture.complete(null);
                LOG.trace("BroadcastNetwork {} - Offline - Ready to be removed", networkId);
                return shutdownCompletionFuture;
            }
            networkStatus = Status.SHUTTING_DOWN;
            LOG.trace("BroadcastNetwork {} - Shutting down - No more connectPeer requests will be accepted", networkId);
        } finally {
            networkLock.unlock();
        }

        routablePeer.forEach(networkPeer -> networkPeer.forceDisconnection(id()));
        return shutdownCompletionFuture;
    }
}
