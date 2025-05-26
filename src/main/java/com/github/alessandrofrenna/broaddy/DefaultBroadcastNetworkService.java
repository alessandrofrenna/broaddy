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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultBroadcastNetworkService implements BroadcastNetworkService {
    private final Logger LOG = LoggerFactory.getLogger(DefaultBroadcastNetworkService.class);

    private final BroadcastNetworkRegistry networkRegistry;

    public DefaultBroadcastNetworkService(BroadcastNetworkRegistry networkRegistry) {
        this.networkRegistry = networkRegistry;
    }

    @Override
    public BroadcastNetwork create(NetworkId<?> networkId) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }

        final Optional<BroadcastNetwork> networkOptional = networkRegistry.find(networkId);
        if (networkOptional.isPresent()) {
            LOG.trace("A network with {} already exists. The existing instance will be returned", networkId);
            return networkOptional.get();
        }

        LOG.trace("Creating BroadcastNetwork with id {}", networkId);
        BroadcastNetwork network = new DefaultBroadcastNetwork(networkId);
        boolean result  = networkRegistry.store(network);
        if (!result) {
            throw new BroadcastNetworkStoreException(networkId);
        }
        LOG.trace("BroadcastNetwork with id {} created and stored inside {}", networkId, networkRegistry);
        return network;
    }

    @Override
    public Optional<BroadcastNetwork> locate(NetworkId<?> networkId) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }
        LOG.trace("Locating BroadcastNetwork with id {}", networkId);
        return networkRegistry.find(networkId);
    }

    @Override
    public boolean dispose(NetworkId<?> networkId) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }
        Optional<BroadcastNetwork> locatedNetwork = locate(networkId);
        if (locatedNetwork.isEmpty()) {
            LOG.trace("BroadcastNetwork with id {} not found, destroy operation is complete", networkId);
            return true;
        }
        LOG.trace("Disposing BroadcastNetwork with id {}", networkId);
        CompletableFuture<Void> shutdownCompletableFuture = locatedNetwork.get().shutdown();
        try {
            shutdownCompletableFuture.get();
            LOG.trace("BroadcastNetwork with id {} is offline", networkId);
            networkRegistry.remove(networkId);
            LOG.trace("Disposed BroadcastNetwork with id {}", networkId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.trace("Dispose operation failed because: {}", e.getMessage(), e);
            return false;
        }
    }
}
