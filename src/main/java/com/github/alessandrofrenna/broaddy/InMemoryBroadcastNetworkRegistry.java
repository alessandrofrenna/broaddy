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

public class InMemoryBroadcastNetworkRegistry implements BroadcastNetworkRegistry {
    private final Map<NetworkId<?>, BroadcastNetwork> networkMap = new ConcurrentHashMap<>();

    @Override
    public boolean store(BroadcastNetwork network) {
        Objects.requireNonNull(network, "network is required, null provided");
        if (networkMap.containsKey(network.id())) {
            throw new DuplicateNetworkIdException(network.id());
        }
        var value = networkMap.put(network.id(), network);
        // value is the previously stored value on the map.
        // when the map doesn't contain the value, put returns null
        return value == null;
    }

    @Override
    public boolean update(NetworkId<?> networkId, BroadcastNetwork network) {
        Objects.requireNonNull(networkId, "networkId is required, null provided");
        Objects.requireNonNull(network, "network is required, null provided");
        if (!Objects.equals(networkId, network.id())) {
            throw new IllegalArgumentException("network.id() returns a value different from the provided networkId");
        }

        if (!networkMap.containsKey(networkId)) {
            // invoke store operation when key is missing
            return store(network);
        }
        var value = networkMap.replace(network.id(), network);
        // value is the previously stored value on the map.
        // when the map doesn't contain the value, replace returns null
        return value != null;
    }

    @Override
    public Optional<BroadcastNetwork> locate(NetworkId<?> networkId) {
        Objects.requireNonNull(networkId, "networkId is required, null provided");
        return Optional.ofNullable(networkMap.getOrDefault(networkId, null));
    }

    @Override
    public void dispose(NetworkId<?> networkId) {
        Objects.requireNonNull(networkId, "networkId is required, null provided");
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
