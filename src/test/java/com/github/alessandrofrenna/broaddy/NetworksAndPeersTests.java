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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetworksAndPeersTests {
    private BroadcastNetwork network;

    record LongId(Long id) implements RoutableId<Long> {
        @Override
        public Long get() {
            return id;
        }
    }

    @BeforeEach
    void setUp() {
        network = new DefaultBroadcastNetwork(new NetworkId.Str("test_network"));
    }

    @Test
    void networkName_shouldBeCorrect() {
        assertNotNull(network.id());
        assertEquals("test_network", network.id().get());
    }

    @Test
    void connectPeer_shouldSucceed() {
        NetworkPeer peer1 = new DefaultNetworkPeer(new RoutableId.UUID());
        boolean joinResult = peer1.join(network, (msg) -> {});
        assertTrue(joinResult);
        assertEquals(1, peer1.countJoinedNetworks());
        assertEquals(1, network.size());
    }

    @Test
    void connectingPeerWithSameId_shouldFail() {
        RoutableId<String> id = new RoutableId.String("test_peer_id");
        NetworkPeer peer1 = new DefaultNetworkPeer(id);
        NetworkPeer peer2 = new DefaultNetworkPeer(id);
        boolean joinResultPeer1 = peer1.join(network, (msg) -> {});
        boolean joinResultPeer2 = peer2.join(network, (msg) -> {});

        assertTrue(joinResultPeer1);
        assertFalse(joinResultPeer2);
        assertEquals(1, network.size());
    }

    @Test
    void broadcastCall_shouldNotifyAllConnectedPeers() {
        AtomicInteger msgCount = new AtomicInteger(0);
        var peer1 = new DefaultNetworkPeer(new RoutableId.UUID());
        var peer2 = new DefaultNetworkPeer(new RoutableId.UUID());
        Consumer<Message<?>> consumer = (Message<?> message) -> {
            if (message.payload() instanceof String payload) {
                msgCount.incrementAndGet();
                assertNotNull(message.id());
                assertEquals("Hello world", payload);
            }
        };
        peer1.join(network, consumer);
        peer2.join(network, consumer);
        assertEquals(2, network.size());
        network.broadcast(new StringMessage("Hello world"));
        assertEquals(2, msgCount.get());
    }

    @Test
    void disconnectPeer_shouldSucceed() {
        NetworkPeer peer1 = new DefaultNetworkPeer(new RoutableId.UUID());
        peer1.join(network, (msg) -> {});
        var leaveResult = peer1.leave(network.id());
        assertTrue(leaveResult);
        assertEquals(0, network.size());

        peer1.join(network, (msg) -> {});
        assertEquals(1, network.size());
        var disconnectResult = network.disconnectPeer(peer1.id());
        assertEquals(BroadcastNetwork.Disconnect.OK, disconnectResult);
        assertEquals(0, network.size());
    }

    @Test
    void disconnectPeer_shouldFailWhenPeerDoesNotExists() {
        RoutableId<String> missingRoutableId = new RoutableId.String("test_missing_peer");
        var disconnectResult = network.disconnectPeer(missingRoutableId);
        assertEquals(BroadcastNetwork.Disconnect.NOT_FOUND, disconnectResult);
    }

    @Test
    void shutdown_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        NetworkPeer peer1 = new DefaultNetworkPeer(new RoutableId.String("test_peer_id_1"));
        NetworkPeer peer2 = new DefaultNetworkPeer(new RoutableId.String("test_peer_id_2"));
        peer1.join(network, (msg) -> {});
        peer2.join(network, (msg) -> {});
        assertEquals(2, network.size());

        var shutdownCF = network.shutdown();
        shutdownCF.thenRun(() -> {
            assertTrue(shutdownCF.isDone());
            assertTrue(network.isEmpty());
            assertEquals(0, peer1.countJoinedNetworks());
            assertEquals(0, peer2.countJoinedNetworks());
            // trying to rejoin the network should fail!
            assertFalse(peer1.join(network, (msg) -> {}));
            assertFalse(peer2.join(network, (msg) -> {}));
            assertEquals(BroadcastNetwork.Connect.NETWORK_OFFLINE, network.connectPeer(peer1));
            assertEquals(BroadcastNetwork.Connect.NETWORK_OFFLINE, network.connectPeer(peer2));
        }).get(5, TimeUnit.SECONDS); //
    }

    @Test
    void registeringPeersWithDifferentIdTypes_shouldSucceed() {
        NetworkPeer peer1 = new DefaultNetworkPeer(new RoutableId.String("test_peer_id_1"));
        NetworkPeer peer2 = new DefaultNetworkPeer(new RoutableId.UUID());
        NetworkPeer peer3 = new DefaultNetworkPeer(new LongId(350L));
        peer1.join(network, (msg) -> {});
        peer1.join(network, (msg) -> {});
        peer2.join(network, (msg) -> {});
        peer2.join(network, (msg) -> {});
        peer3.join(network, (msg) -> {});
        peer3.join(network, (msg) -> {});
        assertEquals(3, network.size());
    }

    @Test
    void peerConnectionToMultipleNetworks_shouldSucceed() {
        BroadcastNetwork network2 = new DefaultBroadcastNetwork(new NetworkId.UUID());
        NetworkPeer peer1 = new DefaultNetworkPeer(new RoutableId.String("test_peer_id_1"));

        peer1.join(network, (msg) -> {});
        peer1.join(network2, (msg) -> {});
        assertEquals(2, peer1.countJoinedNetworks());
        assertFalse(network.isEmpty());
        assertEquals(1, network.size());
        assertFalse(network2.isEmpty());
        assertEquals(1, network2.size());
    }

    @Test
    void callingMethodsOfTheNetworkPeerWithNulls_shouldFail() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultNetworkPeer(null));
        NetworkPeer networkPeer = new DefaultNetworkPeer(new RoutableId.String("test_peer"));
        assertThrows(IllegalArgumentException.class, () -> networkPeer.join(null, (msg) -> {}));
        assertThrows(IllegalArgumentException.class, () -> networkPeer.join(new DefaultBroadcastNetwork(new NetworkId.UUID()), null));
        assertThrows(IllegalArgumentException.class, () -> networkPeer.leave(null));
        assertThrows(IllegalArgumentException.class, () -> networkPeer.forceDisconnection(null));
        assertThrows(IllegalArgumentException.class, () -> networkPeer.deliverMessage(null, new StringMessage("")));
    }

    @Test
    void callingMethodsOfTheBroadcastNetworkWithNulls_shouldFail() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultNetworkPeer(null));
        assertThrows(IllegalArgumentException.class, () -> network.connectPeer(null));
        assertThrows(IllegalArgumentException.class, () -> network.disconnectPeer(null));
    }

}
