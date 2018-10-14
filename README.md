# SimpleNet
An easy-to-use, event-driven, asynchronous, network application framework.

# How can I start using SimpleNet?
 1. Add SimpleNet as a dependency using either Maven or Gradle:
 
 Maven:
 
 ```xml
<dependency>
    <groupId>com.github.jhg023</groupId>
    <artifactId>SimpleNet</artifactId>
    <version>1.2.2</version>
</dependency>
```

Gradle:

    compile 'com.github.jhg023:SimpleNet:1.2.2'
 
 2. To create a `Client`, you can use the following:
```java
// Instantiate a new Client.
Client client = new Client();

// Register one connection listener.
client.onConnect(() -> {
    System.out.println(client + " has connected to the server!");
    
    // Builds a packet and sends it to the server immediately.
    Packet.builder().putByte(1).putInt(42).writeAndFlush(client);
});

// Register one disconnection listener.
client.onDisconnect(() -> System.out.println(client + " has disconnected from the server!"));

// Attempt to connect to a server AFTER registering listeners.
client.connect("localhost", 43594);
```

 3. To create a `Server`, you can use the following:

```java
// Instantiate a new Server.
Server server = new Server();

// Bind the server to an address and port.
server.bind("localhost", 43594);

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

    // Register one disconnection listener.
    client.onDisconnect(() -> System.out.println(client + " has disconnected!"));
});
```

 4. Congratulations, you're finished!
