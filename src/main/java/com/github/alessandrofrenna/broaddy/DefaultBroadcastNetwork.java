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

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultBroadcastNetwork implements BroadcastNetwork {
    private final NetworkId<?> networkId;
    private final Set<NetworkPeer> networkPeers = new CopyOnWriteArraySet<>();
    private final Lock networkLock = new ReentrantLock(true);

    private volatile Status networkStatus;
    private CompletableFuture<Void> shutdownCompletionFuture;

    public DefaultBroadcastNetwork(NetworkId<?> networkId) {
        this.networkId = networkId;
        this.networkStatus = Status.Online;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NetworkId<?> id() {
        return networkId;
    }

    @Override
    public Connect connectPeer(NetworkPeer peer) {
        networkLock.lock();
        try {
            switch (networkStatus) {
                case ShuttingDown -> {
                    return Connect.NETWORK_SHUTTING_DOWN;
                }
                case Offline -> {
                    return Connect.NETWORK_OFFLINE;
                }
            }
        } finally {
            networkLock.unlock();
        }
        if (networkPeers.contains(peer)) {
            return Connect.EXISTING_ID;
        }
        if (networkPeers.add(peer)) {
            return Connect.OK;
        }
        return Connect.FAILED;
    }

    @Override
    public <U> Disconnect disconnectPeer(PeerId<U> peerId) {
        if (networkPeers.removeIf(networkPeer -> networkPeer.id().equals(peerId))) {
            networkLock.lock();
            try {
                if (networkStatus == Status.ShuttingDown && networkPeers.isEmpty() && shutdownCompletionFuture != null && !shutdownCompletionFuture.isDone()) {
                    networkStatus = Status.Offline;
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
        return networkPeers.size();
    }

    @Override
    public boolean isEmpty() {
        return networkPeers.isEmpty();
    }

    @Override
    public <T>  void broadcast(Message<T> message) {
        networkLock.lock();
        try {
            if (networkStatus != Status.Online) {
                return;
            }
        } finally {
            networkLock.unlock();
        }
        networkPeers.forEach(networkPeer -> networkPeer.notify(id(), message));
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        networkLock.lock();
        try {
            if (networkStatus == Status.ShuttingDown) {
                return shutdownCompletionFuture;
            }
            if (networkStatus == Status.Offline) {
                return CompletableFuture.completedFuture(null);
            }
            if (networkPeers.isEmpty()) {
                networkStatus = Status.Offline;
                return CompletableFuture.completedFuture(null);
            }

            networkStatus = Status.ShuttingDown;
            shutdownCompletionFuture = new CompletableFuture<>();
        } finally {
            networkLock.unlock();
        }

        networkPeers.forEach(networkPeer -> networkPeer.forceLeave(id()));

        return shutdownCompletionFuture;
    }
}
