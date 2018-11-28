# SimpleNet
An easy-to-use, event-driven, asynchronous, network application framework.

# How can I start using SimpleNet?
 1. Add SimpleNet as a dependency using either Maven or Gradle:

Maven:

```xml
<dependency>
    <groupId>com.github.jhg023</groupId>
    <artifactId>SimpleNet</artifactId>
    <version>1.2.6</version>
</dependency>
```

Gradle:

    compile 'com.github.jhg023:SimpleNet:1.2.6'

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
