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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BroadcastNetworkServiceMockedTests {
    @Mock
    private InMemoryBroadcastNetworkRegistry registryMock;

    @Mock
    private BroadcastNetwork broadcastNetworkMock;

    private BroadcastNetworkService networkService;

    @BeforeEach
    void setup() {
        networkService = new DefaultBroadcastNetworkService(registryMock);
    }

    @Test
    void createShouldThrow_whenRegistryStoreInvocationReturnsFalse() {
        when(registryMock.store(any())).thenReturn(false);
        assertThrows(BroadcastNetworkStoreException.class, () -> networkService.create(new NetworkId.UUID()));
        verify(registryMock, times(1)).store(any());
    }

    @Test
    void dispose_whenShutdownCompletesExceptionally_shouldReturnFalseAndNotRemoveNetwork() {
        NetworkId<UUID> networkId = new NetworkId.UUID();
        CompletableFuture<Void> failedShutdownFuture = CompletableFuture.failedFuture(new RuntimeException("Simulated shutdown failure!"));

        when(broadcastNetworkMock.shutdown()).thenReturn(failedShutdownFuture);
        when(registryMock.find(networkId)).thenReturn(Optional.of(broadcastNetworkMock));

        boolean result = networkService.dispose(networkId);
        assertFalse(result);
        verify(broadcastNetworkMock).shutdown();
        verify(registryMock, never()).remove(networkId);
    }

    @Test
    void dispose_whenShutdownIsInterrupted_shouldReturnFalseAndNotRemoveNetwork() throws InterruptedException {
        NetworkId<UUID> networkId = new NetworkId.UUID();
        CompletableFuture<Void> blockingShutdownFuture = new CompletableFuture<>(); // Will never complete on its own
        when(broadcastNetworkMock.shutdown()).thenReturn(blockingShutdownFuture);
        when(registryMock.find(networkId)).thenReturn(Optional.of(broadcastNetworkMock));

        AtomicBoolean disposeResult = new AtomicBoolean(true);
        Thread interruptThread = getInterruptThread();
        try {
            disposeResult.set(networkService.dispose(networkId));
        } catch (Exception e) {
            fail("Dispose method threw an unexpected exception: " + e.getMessage());
        } finally {
            interruptThread.join(); // Ensure interrupting thread finishes
            Thread.interrupted();
        }

        assertFalse(disposeResult.get());
        verify(broadcastNetworkMock).shutdown(); // Ensure shutdown was called
        verify(registryMock, never()).remove(networkId); // Ensure network was NOT removed
    }

    private static Thread getInterruptThread() {
        final Thread mainThread = Thread.currentThread();
        Thread interruptThread = new Thread(() -> {
            try {
                // Give the main thread a moment to enter future.get()
                Thread.sleep(100);
                if (mainThread.getState() == Thread.State.WAITING || mainThread.getState() == Thread.State.TIMED_WAITING) {
                    mainThread.interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        interruptThread.start();
        return interruptThread;
    }

}
