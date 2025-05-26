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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryBroadcastNetworkRegistry implements BroadcastNetworkRegistry {
    private final static Logger LOG = LoggerFactory.getLogger(InMemoryBroadcastNetworkRegistry.class);

    private final Map<NetworkId<?>, BroadcastNetwork> networkMap = new ConcurrentHashMap<>();

    @Override
    public boolean store(BroadcastNetwork network) {
        if (Objects.isNull(network)) {
            throw new IllegalArgumentException("network is required, null provided");
        }

        LOG.trace("Storing BroadcastNetwork with id {}", network.id());
        if (networkMap.containsKey(network.id())) {
            LOG.trace("A BroadcastNetwork with id of {} was found, error will be thrown", network.id());
            throw new DuplicateNetworkIdException(network.id());
        }
        var value = networkMap.put(network.id(), network);
        // value is the previously stored value on the map.
        // when the map doesn't contain the value, put returns null
        boolean result = value == null;
        if (result) {
            LOG.trace("BroadcastNetwork with id {} was stored successfully", network.id());
        } else {
            LOG.trace("BroadcastNetwork with id {} was not stored", network.id());
        }
        return result;
    }

    @Override
    public boolean update(NetworkId<?> networkId, BroadcastNetwork network) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }
        if (Objects.isNull(network)) {
            throw new IllegalArgumentException("network is required, null provided");
        }
        if (!Objects.equals(networkId, network.id())) {
            throw new IllegalArgumentException("network.id() returns a value different from the provided networkId");
        }

        LOG.trace("Updating BroadcastNetwork with id {}", network.id());
        if (!networkMap.containsKey(networkId)) {
            LOG.trace("BroadcastNetwork with id {} not found, it will be created", network.id());
            // invoke store operation when key is missing
            return store(network);
        }
        var value = networkMap.replace(network.id(), network);
        // value is the previously stored value on the map.
        // when the map doesn't contain the value, replace returns null
        boolean result = value != null;
        if (result) {
            LOG.trace("BroadcastNetwork with id {} was updated successfully", network.id());
        } else {
            LOG.trace("BroadcastNetwork with id {} was not updated", network.id());
        }
        return result;
    }

    @Override
    public Optional<BroadcastNetwork> find(NetworkId<?> networkId) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }
        return Optional.ofNullable(networkMap.getOrDefault(networkId, null));
    }

    @Override
    public void remove(NetworkId<?> networkId) {
        if (Objects.isNull(networkId)) {
            throw new IllegalArgumentException("networkId is required, null provided");
        }
        LOG.trace("Trying to remove BroadcastNetwork with id {}", networkId);
        networkMap.remove(networkId);
    }

    @Override
    public boolean isEmpty() {
        return networkMap.isEmpty();
    }

    @Override
    public long size() {
        return networkMap.size();
    }
}
