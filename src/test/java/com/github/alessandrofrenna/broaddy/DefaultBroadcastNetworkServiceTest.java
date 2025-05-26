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

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultBroadcastNetworkServiceTest {
    private DefaultBroadcastNetworkService networkService;

    @BeforeEach
    void setup() {
        networkService = new DefaultBroadcastNetworkService(new InMemoryBroadcastNetworkRegistry());
    }

    @Test
    void networkCreation_shouldSucceed() {
        NetworkId<UUID> id = new NetworkId.UUID();
        BroadcastNetwork network = networkService.create(id);
        assertEquals(id, network.id());
    }

    @Test
    void creatingTwoNetworksWithTheSameId_shouldReturnTheFirstNetwork() {
        NetworkId<UUID> id = new NetworkId.UUID();
        final BroadcastNetwork network = networkService.create(id);
        BroadcastNetwork networkDouble = networkService.create(id);
        assertEquals(network, networkDouble);
    }

    @Test
    void networkLocation_shouldSucceed() {
        NetworkId<UUID> id = new NetworkId.UUID();
        BroadcastNetwork network = networkService.create(id);
        Optional<BroadcastNetwork> locatedNet = networkService.locate(id);
        assertTrue(locatedNet.isPresent());
        assertEquals(network, locatedNet.get());
    }

    @Test
    void usingTheIdOfANotStoredNetwork_whenLocateIsInvoked_shouldProduceAndEmptyOptional() {
        NetworkId<UUID> id = new NetworkId.UUID();
        Optional<BroadcastNetwork> locatedNet = networkService.locate(id);
        assertTrue(locatedNet.isEmpty());
    }

    @Test
    void networkDispose_shouldSucceed() {
        NetworkId<UUID> id = new NetworkId.UUID();
        networkService.create(id);
        boolean result = networkService.dispose(id);
        assertTrue(result);
    }

    @Test
    void usingTheIdOfANotStoredNetwork_whenDisposeIsInvoked_shouldReturnTrue() {
        NetworkId<UUID> id = new NetworkId.UUID();
        boolean result = networkService.dispose(id);
        assertTrue(result);
    }

    @Test
    void callingMethodsOfTheServiceWithNulls_shouldFail() {
        assertThrows(IllegalArgumentException.class, () -> networkService.create(null));
        assertThrows(IllegalArgumentException.class, () -> networkService.locate(null));
        assertThrows(IllegalArgumentException.class, () -> networkService.dispose(null));
    }
}
