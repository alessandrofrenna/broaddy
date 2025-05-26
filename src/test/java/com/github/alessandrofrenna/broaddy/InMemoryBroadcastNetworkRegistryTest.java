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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryBroadcastNetworkRegistryTest {
    BroadcastNetworkRegistry networkRegistry;

    @BeforeEach
    void setup() {
        networkRegistry = new InMemoryBroadcastNetworkRegistry();
    }

    @Test
    void storeOperation_shouldSucceed() {
        BroadcastNetwork network = new DefaultBroadcastNetwork(new NetworkId.UUID());
        assertTrue(networkRegistry.isEmpty());
        boolean result = networkRegistry.store(network);
        assertTrue(result);
        assertFalse(networkRegistry.isEmpty());
        assertEquals(1, networkRegistry.size());
    }

    @Test
    void storeOperation_ofNetworksWithSameId_shouldFail() {
        BroadcastNetwork network1 = new DefaultBroadcastNetwork(new NetworkId.String("test_net_id"));
        BroadcastNetwork network2 = new DefaultBroadcastNetwork(new NetworkId.String("test_net_id"));
        networkRegistry.store(network1);
        assertThrows(DuplicateNetworkIdException.class, () -> networkRegistry.store(network2));
    }

    @Test
    void updateOperation_shouldSucceed() {
        BroadcastNetwork network = new DefaultBroadcastNetwork(new NetworkId.UUID());
        assertTrue(networkRegistry.isEmpty());
        boolean result = networkRegistry.update(network.id(), network);
        assertTrue(result);
        assertFalse(networkRegistry.isEmpty());
        assertEquals(1, networkRegistry.size());
    }

    @Test
    void updateOperation_onNetworksWithDifferentIds_shouldFailWithException() {
        BroadcastNetwork network1 = new DefaultBroadcastNetwork(new NetworkId.String("test_net_id"));
        BroadcastNetwork network2 = new DefaultBroadcastNetwork(new NetworkId.UUID());
        networkRegistry.store(network1);

        final var optional1 = networkRegistry.locate(network1.id());
        assertTrue(optional1.isPresent());
        assertEquals(network1, optional1.get());

        assertThrows(IllegalArgumentException.class, () -> networkRegistry.update(network1.id(), network2));
        assertEquals(1, networkRegistry.size());
    }

    @Test
    void updateOperation_ofNetworksWithSameId_shouldReplaceNetwork1WithNetwork2() {
        BroadcastNetwork network1 = new DefaultBroadcastNetwork(new NetworkId.String("test_net_id"));
        BroadcastNetwork network2 = new DefaultBroadcastNetwork(new NetworkId.String("test_net_id"));
        networkRegistry.store(network1);

        final var optional1 = networkRegistry.locate(network1.id());
        assertTrue(optional1.isPresent());
        assertEquals(network1, optional1.get());

        final var updateResult = networkRegistry.update(network1.id(), network2);
        assertTrue(updateResult);

        final var optional2 = networkRegistry.locate(network2.id());
        assertTrue(optional2.isPresent());
        assertEquals(network2, optional2.get());
    }

    @Test
    void loadOperation_shouldReturnAnOptionalWithTheDesiredNetwork() {
        BroadcastNetwork network = new DefaultBroadcastNetwork(new NetworkId.UUID());
        networkRegistry.store(network);
        var optionalLocatedNet = networkRegistry.locate(network.id());
        assertTrue(optionalLocatedNet.isPresent());
        assertEquals(network, optionalLocatedNet.get());
    }

    @Test
    void disposeOperation_shouldSucceed() {
        BroadcastNetwork network = new DefaultBroadcastNetwork(new NetworkId.UUID());
        networkRegistry.store(network);
        assertFalse(networkRegistry.isEmpty());
        networkRegistry.dispose(network.id());
        assertTrue(networkRegistry.isEmpty());
    }

    @Test
    void storeOperation_ofNetworksWithDifferentIdTypes_shouldSucceed() {
        BroadcastNetwork network1 = new DefaultBroadcastNetwork(new NetworkId.String("test_net_id"));
        BroadcastNetwork network2 = new DefaultBroadcastNetwork(new NetworkId.UUID());
        networkRegistry.store(network1);
        networkRegistry.store(network2);
        assertEquals(2, networkRegistry.size());

        var optionalNet1 = networkRegistry.locate(network1.id());
        var optionalNet2 = networkRegistry.locate(network2.id());
        assertTrue(optionalNet1.isPresent());
        assertTrue(optionalNet2.isPresent());
        assertEquals(network1, optionalNet1.get());
        assertEquals(network2, optionalNet2.get());
    }

    @Test
    void locateOperation_ofAMissingNetworkId_shouldProduceAnEmptyOptional() {
        NetworkId<String> id = new NetworkId.String("test_net_id");
        var optionalNet = networkRegistry.locate(id);
        assertTrue(optionalNet.isEmpty());
    }

    @Test
    void callingMethodsOfTheRegistryWithNulls_shouldFail() {
        assertThrows(NullPointerException.class, () -> networkRegistry.store(null));
        assertThrows(NullPointerException.class, () -> networkRegistry.update(null, new DefaultBroadcastNetwork(new NetworkId.UUID())));
        assertThrows(NullPointerException.class, () -> networkRegistry.update(new NetworkId.UUID(), null));
        assertThrows(NullPointerException.class, () -> networkRegistry.locate(null));
        assertThrows(NullPointerException.class, () -> networkRegistry.dispose(null));
    }

}
