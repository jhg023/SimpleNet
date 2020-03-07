<img src="https://maven-badges.herokuapp.com/maven-central/com.github.jhg023/SimpleNet/badge.svg"> <img src="http://githubbadges.com/star.svg?user=jhg023&repo=SimpleNet&background=0000ff&color=ffffff&style=flat">

A version of SimpleNet that utilizes virtual threads, enabling efficient and scalable blocking I/O. For more information, please read the [v1 README](https://github.com/jhg023/SimpleNet/blob/master/README.md).

The [latest (2020/02/22)](http://jdk.java.net/loom/) early access build of Project Loom is required to build this branch.

## Client Example
```java
// Instantiate a new client.
var client = new Client();

// Register a disconnect listener.
client.onDisconnect(() -> {
    System.out.println(client + " disconnected from the server!");
});

// Attempt to connect to a server AFTER registering listener(s).
client.connect("localhost", 43594);

// Builds a packet and submits it to the queue.
// This packet is not sent to the server until the queue is flushed.
Packet.builder().putByte(1).putInt(42).queue(client);

// Builds a packet, submits it to the queue, and flushes the queue.
// Flushing the queue sends both packets to the server.
Packet.builder().putByte(2).putLong(123).queueAndFlush(client);
```
## Server Example
```java
// Instantiate a new server.
var server = new Server();

// Register one connection listener.
server.onConnect(client -> {
    System.out.println(client + " has connected!");

    // Register a disconnect listener.
    client.onDisconnect(() -> {
        System.out.println(client + " has disconnected!");
    });

    while (true) {
        int opcode = client.readByte(); // 1, then 2

        switch (opcode) {
            case 1:
                System.out.println(client.readInt());  // 42
                break;
            case 2:
                System.out.println(client.readLong()); // 123
                break;
        }
    }
});

// Bind the server to an address and port AFTER registering listener(s).
server.bind("localhost", 43594);
```