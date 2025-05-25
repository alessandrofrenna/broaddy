# Broaddy
Broaddy is a lightweight library that allows the creation of small broadcast networks of objects.</br>
It is a simple implementation of the [Observer design pattern](https://en.wikipedia.org/wiki/Observer_pattern).

## What is a BroadcastNetwork?
A `BroadcastNetwork` is a centralized network of Java POJOs that uses the [Observer design pattern](https://en.wikipedia.org/wiki/Observer_pattern) to send messages to its members.
A member of the `BroadcastNetwork` is called a `NetworkPeer`.</br>
A `NetworkPeer` can subscribe to multiple `BroadcastNetwork`s.

# Licensing
This project is licensed under the [Apache License v2.0](https://www.apache.org/licenses/LICENSE-2.0).