<img src="https://maven-badges.herokuapp.com/maven-central/com.github.jhg023/SimpleNet/badge.svg"> <img src="http://githubbadges.com/star.svg?user=jhg023&repo=SimpleNet&background=0000ff&color=ffffff&style=flat">

# What is SimpleNet?
SimpleNet is a simplistic, client-server framework written in Java. One or more `Client` objects can connect to a `Server` and send data back-and-forth via TCP. Most methods that read data from the network are non-blocking and are invoked asynchronously when the requested data arrives. Not having to block a thread and wait for data is what makes SimpleNet scalable for different types of applications such as chat servers, multiplayer game servers, and so much more!
# Maven/Gradle Dependency
 1. Add SimpleNet as a dependency using either Maven or Gradle:

Maven:

```xml
<dependency>
    <groupId>com.github.jhg023</groupId>
    <artifactId>SimpleNet</artifactId>
    <version>1.6.6</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.github.jhg023:SimpleNet:1.6.6'
```

 2. Because SimpleNet is compiled with Java 11, you must first require its module in your `module-info.java`:

```java
module my.project {
    requires com.github.simplenet;
}
```
# What do I need to know before using SimpleNet?
- As stated above, SimpleNet is mostly callback-based. This means that you can request a specific amount of bytes, `n`, and specify what should be done with those `n` bytes, as if the `Client` or `Server` has already received them.
```java
// Request a single byte and print it out when it arrives.
client.readByte(System.out::println);

// Continuously request a single byte and print it out when it arrives.
// 
// After the callback completes, another byte will be requested automatically, 
// and the same callback will be invoked. This continues until the connection is closed.
client.readByteAlways(System.out::println);
```
- Packets are sent from a `Client` to a `Server` (and vice versa) via the `Packet` class.
```java
// Create a packet that contains two bytes (with the value 42) and send it to a client.
Packet.builder().putByte(42).putByte(42).queueAndFlush(client);

// Create two packets with the specified data and queue them (but don't flush) to a client.
Packet.builder().putInt(123456).queue(client);
Packet.builder().putString("Hello World!").queue(client);

// Flush the queued packets to the client (these packets will be transformed into a single,
// big packet to improve throughput.
// 
// This method only needs to be called when using Packet#queue and not Packet#queueAndFlush.
client.flush();
```
# Client Example
```java
// Instantiate a new client.
var client = new Client();

// Register a connection listener.
client.onConnect(() -> {
    System.out.println(client + " has connected to the server!");
    
    // Builds a packet and sends it to the server immediately.
    Packet.builder().putByte(1).putInt(42).queueAndFlush(client);
});

// Register a pre-disconnection listener.
client.preDisconnect(() -> System.out.println(client + " is about to disconnect from the server!"));

// Register a post-disconnection listener.
client.postDisconnect(() -> System.out.println(client + " successfully disconnected from the server!"));

// Attempt to connect to a server AFTER registering listeners.
client.connect("localhost", 43594);
```
# Server Example
```java
// Instantiate a new server.
var server = new Server();

// Register one connection listener.
server.onConnect(client -> {
    System.out.println(client + " has connected!");

    /*
     * When one byte arrives from the client, switch on it.
     * If the byte equals 1, then request an int and print it
     * when it arrives.
     *
     * Because `readByteAlways` is used, this will loop when
     * the callback completes, but does not hang the executing thread.
     */
    client.readByteAlways(opcode -> {
        switch (opcode) {
            case 1:
                client.readInt(System.out::println);
        }
    });

    // Register a pre-disconnection listener.
    client.preDisconnect(() -> System.out.println(client + " is about to disconnect!"));

    // Register a post-disconnection listener.
    client.postDisconnect(() -> System.out.println(client + " has successfully disconnected!"));
});

// Bind the server to an address and port AFTER registering listeners.
server.bind("localhost", 43594);
```
# Chat Server Example
 To emphasize how easy it is to use SimpleNet, I have written an implementation of a scalable chat server below. Note how only two classes are required, `ChatServer` and `ChatClient`. Ideally, a chat server should use a GUI and not the console, as this leads to message cut-off in the window, but this is only to serve as a proof-of-concept.
 
 The full `ChatServer` implementation is as follows:
```java
public class ChatServer {
    public static void main(String[] args) {
        // Initialize a new server.
        var server = new Server();
        
        // Create a map that will keep track of nicknames on our chat server.
        var nicknameMap = new ConcurrentHashMap<Client, String>();
        
        // This callback is invoked when a client connects to this server.
        server.onConnect(client -> {
            // When a client disconnects, remove them from the nickname map.
            client.postDisconnect(() -> nicknameMap.remove(client));
            
            // Repeatedly read a single byte.
            client.readByteAlways(opcode -> {
                switch (opcode) {
                    case 1: // Change nickname
                        client.readString(nickname -> nicknameMap.put(client, nickname));
                        break;
                    case 2: // Send message to connected clients.
                        client.readString(message -> {
                            message = nicknameMap.get(client) + ": " + message;
                            server.queueAndFlushToAllExcept(Packet.builder().putString(message), client);
                        });
                        break;    
                }
            });
        });
        
        // Bind the server to our local network AFTER registering listeners.
        server.bind("localhost", 43594);
    }
}
```
 The full `ChatClient` implementation is as follows:
```java
public class ChatClient {
    public static void main(String[] args) {
        // Initialize a new client.
        var client = new Client();
        
        // This callback is invoked when this client connects to a server.
        client.onConnect(() -> {
            var scanner = new Scanner(System.in);
            
            // If messages arrive from other clients, print them to the console.
            client.readStringAlways(System.out::println);
            
            System.out.print("Enter your nickname: ");
            Packet.builder().putByte(1).putString(scanner.nextLine()).queueAndFlush(client);
            
            // Infinite loop to accept user-input for the chat server.
            while (true) {
                System.out.print("> ");
                
                // Read the client's message from the console.
                var message = scanner.nextLine();
                
                // If this client types "/leave", close their connection to the server.
                if ("/leave".equals(message)) {
                    client.close();
                    break;
                }
                
                // Otherwise, send a packet to the server containing the client's message.
                Packet.builder().putByte(2).putString(message).queueAndFlush(client);
            }
        });
        
        // Attempt to connect to a server AFTER registering listeners.
        client.connect("localhost", 43594);
    }
}
```

# FAQ (Frequently-Asked Questions)
- Can data be sent and received outside of the `onConnect` callback?
  - Yes, but only if the client is already connected to the server. However, this is not recommended unless you're well-versed in concurrency, as the `onConnect` callback is performed asynchronously.
- Can a SimpleNet `Client` be used with some unrelated server and vice-versa?
  - Absolutely!
- I have large packets that exceed the default buffer size; what can I do to avoid an exception?
  - Ideally, the best option would be to split your single, large packet into multiple, small packets. 
  - If splitting the packet is not possible for any reason, both `Client` and `Server` have an overloaded constructor that accepts a buffer size in bytes. You can simply specify a size larger than 8192 (the default size).
- Will Java 8 ever be supported again?
  - No, as Java 8 is no longer supported commercially as of January, 2019. SimpleNet will do its best to keep up with LTS releases. However, you're free to clone the project and build it on an older version of Java, as not many code changes are required.
- What's next for SimpleNet?
  - Once Project Loom is complete and integrated into the mainline JDK, SimpleNet will be rewritten entirely; blocking I/O will be using fibers at that point, which will be much more scalable than my current implementation that uses a fixed thread pool.
