# SimpleNet
An easy-to-use, event-driven, asynchronous, network application framework compiled with Java 10.

# How can I start using SimpleNet?
 1. Because SimpleNet is compiled with Java 10, you'll first need to build it into a dependency (coming to Maven soon) and add it to your build path.
 2. Next, you must edit your `module-info.java` to include the following:

```java
requires SimpleNet;
```
 3. To create a `Client`, you can use the following:
```java
// Instantiate a new Client.
Client client = new Client();

// Register one connection and disconnection listener.
client.onConnect(channel -> System.out.println(channel + " has connected to the server!"));
client.onDisconnect(channel -> System.out.println(channel + " has disconnected from the server!"));

// Attempt to connect to a server.
client.connect("localhost", 43_594);

// Builds a packet and sends it to the server immediately.
Packet.builder().putByte(1).putInt(42).writeAndFlush(client);
```

 4. To create a `Server`, you can use the following:

```java
// Instantiate a new Server.
Server server = new Server();

// Register one connection and disconnection listener.
server.onConnect(channel -> System.out.println(channel + " has connected!"));
server.onDisconnect(channel -> System.out.println(channel + " has disconnected!"));

// Bind the server to an address and port.
server.bind("localhost", 43_594);

/* 
 * When 1 byte arrives from any client, switch on it.
 * If the byte equals 1, then "request" 4 bytes and
 * print them as an int whenever they arrive.
 * 
 * Because `readAlways` is used, the server will always
 * attempt to read one byte.
 */
server.readAlways(1, header -> {
    switch (header.get()) {
        case 1:
            server.read(4, payload -> System.out.println(payload.getInt()));
    }
});
```

 5. Congratulations, you're finished! Be sure to run the `Server` first, as every `Client` will not be able to connect otherwise.
