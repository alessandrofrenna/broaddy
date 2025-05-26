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

public interface RoutableId<T> extends Comparable<RoutableId<T>> {
    T get();

    record String(java.lang.String id) implements RoutableId<java.lang.String> {
        public String {
            Objects.requireNonNull(id, "id is required but it is null");
        }

        @Override
        public java.lang.String get() {
            return id();
        }

        @Override
        public int compareTo(RoutableId<java.lang.String> other) {
            return get().compareTo(other.get());
        }
    }

    record UUID(java.util.UUID uuid) implements RoutableId<java.util.UUID> {

        public UUID {
            Objects.requireNonNull(uuid, "id is required, null provided");
        }

        public UUID() {
            this(java.util.UUID.randomUUID());
        }

        @Override
        public java.util.UUID get() {
            return uuid();
        }

        @Override
        public int compareTo(RoutableId<java.util.UUID> other) {
            return get().compareTo(other.get());
        }
    }
}
