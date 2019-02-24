<img src="https://maven-badges.herokuapp.com/maven-central/com.github.jhg023/SimpleNet/badge.svg"> <img src="http://githubbadges.com/star.svg?user=jhg023&repo=SimpleNet&background=0000ff&color=ffffff&style=flat">
# What is SimpleNet?
An easy-to-use, event-driven, asynchronous, network application framework. SimpleNet provides a built-in `Server` class which the user can instantiate and bind to an existing IP address and port. The user can then create one or more `Client` instances and connect to their `Server` using the same IP address and port.

# How can I start using SimpleNet?
 1. Add SimpleNet as a dependency using either Maven or Gradle:

Maven:

```xml
<dependency>
    <groupId>com.github.jhg023</groupId>
    <artifactId>SimpleNet</artifactId>
    <version>1.4.6</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.github.jhg023:SimpleNet:1.4.6'
```

 2. Because SimpleNet is compiled with Java 11, you must first require its module in your `module-info.java`:

```java
module my.project {
    requires SimpleNet;
}
```

 3. To create a `Client`, you can use the following:
```java
// Instantiate a new Client.
var client = new Client();

// Register a connection listener.
client.onConnect(() -> {
    System.out.println(client + " has connected to the server!");
    
    // Builds a packet and sends it to the server immediately.
    Packet.builder().putByte(1).putInt(42).writeAndFlush(client);
});

// Register an optional pre-disconnection listener.
client.preDisconnect(() -> System.out.println(client + " is about to disconnect from the server!"));

// Register an optional post-disconnection listener.
client.postDisconnect(() -> System.out.println(client + " successfully disconnected from the server!"));

// Attempt to connect to a server AFTER registering listeners.
client.connect("localhost", 43594);
```

 4. To create a `Server`, you can use the following:

```java
// Instantiate a new Server.
var server = new Server();

// Register one connection listener.
server.onConnect(client -> {
    System.out.println(client + " has connected!");

    /*
     * When one byte arrives from the client, switch on it.
     * If the byte equals 1, then print an int when it arrives.
     *
     * Because `readByteAlways` is used, this will loop when
     * the callback completes, but does not hang the main thread.
     */
    client.readByteAlways(opcode -> {
        switch (opcode) {
            case 1:
                client.readInt(System.out::println);
        }
    });

    // Register an optional pre-disconnection listener.
    client.preDisconnect(() -> System.out.println(client + " is about to disconnect!"));

    // Register an optional post-disconnection listener.
    client.postDisconnect(() -> System.out.println(client + " has successfully disconnected!"));
});

// Bind the server to an address and port AFTER registering listeners.
server.bind("localhost", 43594);
```

 5. Congratulations, you're finished!

# FAQ (Frequently-Asked Questions)
- Can data be sent and received outside of the `onConnect` callback?
  - Yes, but only if the client is already connected to the server. However, this is not recommended unless you're well-versed in concurrency, as the `onConnect` callback is performed asynchronously.
- Can a SimpleNet `Client` be used with some unrelated server and vice-versa?
  - Absolutely!
- I have large packets that exceed the default buffer size; what can I do to avoid an exception?
  - Ideally, the best option would be to split your single, large packet into multiple, small packets. 
  - If splitting the packet is not possible for any reason, both `Client` and `Server` have an overloaded constructor that accepts a buffer size in bytes. You can simply specify a size larger than 4096 (the default size).
- Will Java 8 ever be supported again?
  - No, as Java 8 is approaching its EOL (January, 2019). SimpleNet will do its best to keep up with LTS releases.
- What's next for SimpleNet?
  - Once Project Loom is complete and integrated into the mainline JDK, SimpleNet will be rewritten entirely; blocking I/O will be using fibers at that point, which will be much more scalable than my current implementation that uses a thread pool.
