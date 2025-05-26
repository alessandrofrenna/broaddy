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

import java.util.concurrent.CompletableFuture;

public interface BroadcastNetwork {
    NetworkId<?> id();
    Connect connectPeer(NetworkPeer peer);
    <U> Disconnect disconnectPeer(PeerId<U> peerId);
    long size();
    boolean isEmpty();
    <T> void broadcast(Message<T> message);
    CompletableFuture<Void> shutdown();

    enum Status {
        ONLINE, SHUTTING_DOWN, OFFLINE
    }

    enum Connect {
        OK("Peer connected"),
        EXISTING_ID("Peer with the same id is already connected"),
        NETWORK_SHUTTING_DOWN("Peer not connected, network is shutting down"),
        NETWORK_OFFLINE("Peer not connected, network is offline"),
        FAILED("Peer connection failed");

        final String description;

        Connect(String description) {
            this.description = description;
        }

        @SuppressWarnings("unused")
        public String description() {
            return description;
        }
    }

    enum Disconnect {
        OK("Peer disconnected"),
        NOT_FOUND("Peer not found");

        final String description;

        Disconnect(String description) {
            this.description = description;
        }

        @SuppressWarnings("unused")
        public String description() {
            return description;
        }
    }
}
