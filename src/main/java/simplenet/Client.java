/*
 * MIT License
 *
 * Copyright (c) 2019 Jacob Glickman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package simplenet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import pbbl.ByteBufferPool;
import pbbl.direct.DirectByteBufferPool;
import simplenet.channel.Channeled;
import simplenet.packet.Packet;
import simplenet.receiver.Receiver;
import simplenet.utility.IntPair;
import simplenet.utility.MutableInt;
import simplenet.utility.Tuple;
import simplenet.utility.Utility;
import simplenet.utility.data.BooleanReader;
import simplenet.utility.data.ByteReader;
import simplenet.utility.data.CharReader;
import simplenet.utility.data.DataReader;
import simplenet.utility.data.DoubleReader;
import simplenet.utility.data.FloatReader;
import simplenet.utility.data.IntReader;
import simplenet.utility.data.LongReader;
import simplenet.utility.data.StringReader;

/**
 * The entity that will connect to the {@link Server}.
 *
 * @author Jacob G.
 * @since November 1, 2017
 */
public class Client extends Receiver<Runnable> implements Channeled<AsynchronousSocketChannel>, DataReader,
        BooleanReader, ByteReader, CharReader, IntReader, FloatReader, LongReader, DoubleReader, StringReader {

    /**
     * The {@link CompletionHandler} used to process bytes when they are received by this {@link Client}.
     */
    static class Listener implements CompletionHandler<Integer, Client> {

        /**
         * A {@code static} instance of this class to be reused.
         */
        private static final Listener CLIENT_INSTANCE = new Listener();
    
        static final Listener SERVER_INSTANCE = new Listener() {
            @Override
            public void failed(Throwable t, Client client) {
                // It's important that we close the client here ONLY if the stacktrace's message begins with this
                // specific message, as it means that the client has disconnected without the server properly closing
                // it. If we weren't to close the client here, then its disconnect listeners would not run. However,
                // if we always close the client here, then its disconnect listeners will run twice, which we do not
                // want to occur.
                String message;
                
                if ((message = t.getMessage()) == null) {
                    return;
                }
                
                if (message.startsWith("The specified network name is no longer available.")) {
                    client.close();
                }
            }
        };
        
        @Override
        public void completed(Integer result, Client client) {
            // A result of -1 normally means that the end-of-stream has been reached. In that case, close the
            // client's connection.
            int bytesReceived = result;
            
            if (bytesReceived == -1) {
                client.close();
                return;
            }
            
            synchronized (client.buffer) {
                client.size.add(bytesReceived);
                
                var buffer = client.buffer.flip();
                var queue = client.queue;
                
                IntPair<Consumer<ByteBuffer>> peek;
    
                if ((peek = queue.pollLast()) == null) {
                    client.channel.read(buffer.position(client.size.get()).limit(buffer.capacity()), client, this);
                    return;
                }
    
                client.prepend = true;
    
                var shouldDecrypt = client.decryption != null;
                var stack = client.stack;
                int key;
                
                while (client.size.get() >= (key = peek.key)) {
                    client.size.add(-key);
                    
                    var data = new byte[key];
    
                    buffer.get(data);
                    
                    if (shouldDecrypt) {
                        try {
                            data = client.decryption.doFinal(data);
                        } catch (Exception e) {
                            throw new IllegalStateException("An exception occurred whilst encrypting data:", e);
                        }
                    }
    
                    ByteBuffer wrappedBuffer = ByteBuffer.wrap(data);
                    
                    peek.value.accept(wrappedBuffer);
    
                    // TODO: After logging is added, warn the user if wrappedBuffer.hasRemaining() is true.
        
                    while (!stack.isEmpty()) {
                        queue.offerLast(stack.pop());
                    }
        
                    if ((peek = queue.pollLast()) == null) {
                        break;
                    }
                }
    
                client.prepend = false;
    
                if (peek != null) {
                    queue.offerLast(peek);
                }
                
                if (client.size.get() > 0) {
                    buffer.compact().position(client.size.get()).limit(buffer.capacity());
                } else {
                    buffer.clear();
                }
                
                client.channel.read(buffer, client, this);
            }
        }

        @Override
        public void failed(Throwable t, Client client) {
            client.close();
        }
    }

    /**
     * The {@link CompletionHandler} used when this {@link Client} sends one or more {@link Packet}s to a
     * {@link Server}.
     */
    private static final CompletionHandler<Integer, Tuple<Client, ByteBuffer>> PACKET_HANDLER = new CompletionHandler<>() {
        @Override
        public void completed(Integer result, Tuple<Client, ByteBuffer> tuple) {
            var client = tuple.key;
            var buffer = tuple.value;
            
            synchronized (client.packetsToFlush) {
                DIRECT_BUFFER_POOL.give(buffer);
                
                if (!client.channel.isOpen()) {
                    client.outgoingPackets.clear();
                    client.packetsToFlush.clear();
                    return;
                }
                
                var payload = client.packetsToFlush.pollLast();
    
                if (payload == null) {
                    client.writing.set(false);
                    return;
                }
    
                client.channel.write(payload, new Tuple<>(client, payload), this);
            }
        }

        @Override
        public void failed(Throwable t, Tuple<Client, ByteBuffer> tuple) {
            t.printStackTrace();
        }
    };
    
    /**
     * A {@link ByteBufferPool} that dispatches reusable {@code DirectByteBuffer}s.
     */
    private static final ByteBufferPool DIRECT_BUFFER_POOL = new DirectByteBufferPool();
    
    /**
     * The {@link ByteBuffer} that will hold data sent by the {@link Client} or {@link Server}.
     */
    final ByteBuffer buffer;
    
    /**
     * A thread-safe method of keeping track whether this {@link Client} is currently writing data to the network.
     */
    private final AtomicBoolean writing;
    
    /**
     * The amount of readable bytes that currently exist within this {@link Client}'s {@code buffer}.
     */
    private final MutableInt size;
    
    /**
     * A {@link Queue} to manage outgoing {@link Packet}s.
     */
    private final Queue<Packet> outgoingPackets;

    /**
     * A {@link Deque} to manage {@link Packet}s that should be flushed as soon as possible.
     */
    private final Deque<ByteBuffer> packetsToFlush;

    /**
     * The {@link Deque} that keeps track of nested calls to {@link Client#read(int, Consumer, ByteOrder)} and assures that
     * they will complete in the expected order.
     */
    private final Deque<IntPair<Consumer<ByteBuffer>>> stack;

    /**
     * The {@link Deque} used when requesting a certain amount of bytes from the {@link Client} or {@link Server}.
     */
    private final Deque<IntPair<Consumer<ByteBuffer>>> queue;

    /**
     * Whether or not new elements added {@code queue} should be added to the front rather than the back.
     */
    private boolean prepend;
    
    /**
     * The {@link Cipher} used for {@link Packet} encryption.
     */
    private Cipher encryption;
    
    /**
     * The {@link Cipher} used for {@link Packet} decryption.
     */
    private Cipher decryption;
    
    /**
     * The {@link Server} that this {@link Client} is connected to.
     */
    private Server server;

    /**
     * The backing {@link ThreadPoolExecutor} used for I/O.
     */
    private ThreadPoolExecutor executor;

    /**
     * The backing {@link Channel} of a {@link Client}.
     */
    private AsynchronousSocketChannel channel;
    
    /**
     * A {@code package-private} constructor that is used to represent a {@link Client} that is connected to a
     * {@link Server}.
     * <br><br>
     * This is primarily used to keep track of the {@link Client}s that are connected to a {@link Server}.
     *
     * @param bufferSize The size of this {@link Client}'s buffer, in {@code byte}s.
     * @param channel    The channel to back this {@link Client} with.
     * @param server     The {@link Server} that this {@link Client} is connected to.
     */
    Client(int bufferSize, AsynchronousSocketChannel channel, Server server) {
        this(bufferSize, channel);
        this.server = server;
    }
    
    /**
     * Instantiates a new {@link Client} by attempting to open the backing {@link AsynchronousSocketChannel} with a
     * default buffer size of {@code 4,096} bytes.
     */
    public Client() {
        this(4_096);
    }

    /**
     * Instantiates a new {@link Client} by attempting to open the backing {@link AsynchronousSocketChannel} with a
     * provided buffer size in bytes.
     *
     * @param bufferSize The size of this {@link Client}'s buffer, in bytes.
     */
    public Client(int bufferSize) {
        this(bufferSize, null);
    }

    /**
     * Instantiates a new {@link Client} with an existing {@link AsynchronousSocketChannel} with a provided buffer
     * size in bytes.
     *
     * @param bufferSize The size of this {@link Client}'s buffer, in bytes.
     * @param channel    The channel to back this {@link Client} with.
     */
    public Client(int bufferSize, AsynchronousSocketChannel channel) {
        super(bufferSize);
        
        size = new MutableInt();
        writing = new AtomicBoolean();
        outgoingPackets = new ConcurrentLinkedDeque<>();
        packetsToFlush = new ArrayDeque<>();
        queue = new ArrayDeque<>();
        stack = new ArrayDeque<>();
        buffer = ByteBuffer.allocateDirect(bufferSize);
        
        if (channel != null) {
            this.channel = channel;
        }
    }
    
    /**
     * Instantiates a new {@link Client} (whose fields directly refer to the fields of the specified {@link Client})
     * from an existing {@link Client}, essentially acting as a copy-constructor.
     * <br><br>
     * This exists so that, if a user creates a class that extends {@link Client}, they can pass in an existing
     * {@link Client}, allowing them to invoke {@code super(client)} inside their constructor. Doing so will allow
     * them to invoke {@code wrapper.readByte()} (for example) instead of {@code wrapper.getClient().readByte()}.
     *
     * @param client An existing {@link Client} whose backing {@link AsynchronousSocketChannel} is already connected.
     */
    public Client(Client client) {
        super(client);
        
        this.buffer = client.buffer;
        this.writing = client.writing;
        this.outgoingPackets = client.outgoingPackets;
        this.packetsToFlush = client.packetsToFlush;
        this.stack = client.stack;
        this.queue = client.queue;
        this.prepend = client.prepend;
        this.encryption = client.encryption;
        this.decryption = client.decryption;
        this.server = client.server;
        this.executor = client.executor;
        this.channel = client.channel;
        this.size = client.size;
    }

    /**
     * Attempts to connect to a {@link Server} with the specified {@code address} and {@code port} and a default
     * timeout of {@code 30} seconds.
     *
     * @param address The IP address to connect to.
     * @param port    The port to connect to {@code 0 <= port <= 65535}.
     * @throws IllegalArgumentException  If {@code port} is less than 0 or greater than 65535.
     * @throws AlreadyConnectedException If a {@link Client} is already connected to any address/port.
     */
    public final void connect(String address, int port) {
        connect(address, port, 30L, TimeUnit.SECONDS, () ->
            System.err.println("Couldn't connect to the server! Maybe it's offline?"));
    }

    /**
     * Attempts to connect to a {@link Server} with the specified {@code address} and {@code port} and a specified
     * timeout. If the timeout is reached, then the {@link Runnable} is run and the backing
     * {@link AsynchronousSocketChannel} is closed.
     *
     * @param address   The IP address to connect to.
     * @param port      The port to connect to {@code 0 <= port <= 65535}.
     * @param timeout   The timeout value.
     * @param unit      The timeout unit.
     * @param onTimeout The {@link Runnable} that runs if this connection attempt times out.
     */
    public final void connect(String address, int port, long timeout, TimeUnit unit, Runnable onTimeout) {
        Objects.requireNonNull(address);

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The specified port must be between 0 and 65535!");
        }

        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(false);
            thread.setName(thread.getName().replace("Thread", "SimpleNet"));
            thread.setUncaughtExceptionHandler(($, throwable) -> throwable.printStackTrace());
            return thread;
        });

        executor.prestartAllCoreThreads();

        try {
            this.channel = AsynchronousSocketChannel.open(AsynchronousChannelGroup.withThreadPool(executor));
            this.channel.setOption(StandardSocketOptions.SO_RCVBUF, bufferSize);
            this.channel.setOption(StandardSocketOptions.SO_SNDBUF, bufferSize);
            this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            this.channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open the channel:", e);
        }

        try {
            channel.connect(new InetSocketAddress(address, port)).get(timeout, unit);
        } catch (AlreadyConnectedException e) {
            throw new IllegalStateException("This client is already connected to a server:", e);
        } catch (Exception e) {
            onTimeout.run();
            close();
            return;
        }

        try {
            channel.read(buffer, this, Listener.CLIENT_INSTANCE);
        } catch (ShutdownChannelGroupException e) {
            // This exception is caught whenever a client closes their connection to the server. In that case, do
            // nothing.
        }
    
        connectListeners.forEach(Runnable::run);
    }

    /**
     * Closes this {@link Client}'s backing {@link AsynchronousSocketChannel} after flushing any queued packets.
     * <br><br>
     * Any registered pre-disconnect listeners are fired before remaining packets are flushed, and registered
     * post-disconnect listeners are fired after the backing channel has closed successfully.
     * <br><br>
     * Listeners are fired in the same order that they were registered in.
     */
    @Override
    public final void close() {
        preDisconnectListeners.forEach(Runnable::run);

        flush();

        while (writing.get()) {
            Thread.onSpinWait();
        }

        Channeled.super.close();

        if (executor != null) {
            executor.shutdownNow();
        }

        while (channel.isOpen()) {
            Thread.onSpinWait();
        }

        if (server != null) {
            server.connectedClients.remove(this);
        }
        
        postDisconnectListeners.forEach(Runnable::run);
    }

    /**
     * Registers a listener that fires right before a {@link Client} disconnects from a {@link Server}.
     * <br><br>
     * Calling this method more than once registers multiple listeners.
     * <br><br>
     * If this {@link Client}'s connection to a {@link Server} is lost unexpectedly, then its backing
     * {@link AsynchronousSocketChannel} may already be closed.
     *
     * @param listener A {@link Runnable}.
     */
    public final void preDisconnect(Runnable listener) {
        preDisconnectListeners.add(listener);
    }

    /**
     * Registers a listener that fires right after a {@link Client} disconnects from a {@link Server}.
     * <br><br>
     * Calling this method more than once registers multiple listeners.
     *
     * @param listener A {@link Runnable}.
     */
    public final void postDisconnect(Runnable listener) {
        postDisconnectListeners.add(listener);
    }
    
    @Override
    public final void read(int n, Consumer<ByteBuffer> consumer, ByteOrder order) {
        boolean shouldDecrypt = decryption != null;
        
        if (shouldDecrypt) {
            n = Utility.roundUpToNextMultiple(n, decryption.getBlockSize());
        }
        
        synchronized (buffer) {
            if (size.get() >= n && queue.isEmpty() && stack.isEmpty()) {
                size.add(-n);
                
                var data = new byte[n];
    
                buffer.order(order).get(data);
                
                if (shouldDecrypt) {
                    try {
                        data = decryption.doFinal(data);
                    } catch (Exception e) {
                        throw new IllegalStateException("An exception occurred whilst decrypting data:", e);
                    }
                }
                
                var wrappedBuffer = ByteBuffer.wrap(data).order(order);
                
                consumer.accept(wrappedBuffer);
    
                // TODO: After logging is added, warn the user if wrappedBuffer.hasRemaining() is true.
                return;
            }
            
            var pair = new IntPair<Consumer<ByteBuffer>>(n, buffer -> consumer.accept(buffer.order(order)));
            
            if (prepend) {
                stack.push(pair);
            } else {
                queue.offerFirst(pair);
            }
        }
    }

    /**
     * Flushes any queued {@link Packet}s held within the internal {@link Queue}.
     * <br><br>
     * Any {@link Packet}s queued after the call to this method will not be flushed until this method is called again.
     */
    public final void flush() {
        int totalBytes = 0;
        int amountToPoll = outgoingPackets.size();
        
        Packet packet;

        boolean shouldEncrypt = encryption != null;
        
        var queue = new ArrayDeque<byte[]>();

        while (amountToPoll-- > 0 && (packet = outgoingPackets.poll()) != null) {
            int currentBytes = totalBytes;

            boolean tooBig = (totalBytes += packet.getSize(this)) >= bufferSize;
            boolean empty = outgoingPackets.isEmpty();

            if (!tooBig || empty) {
                queue.addAll(packet.getQueue());
            }

            // If we've buffered all of the packets that we can, send them off.
            if (tooBig || empty) {
                var raw = DIRECT_BUFFER_POOL.take(empty ? totalBytes : currentBytes);
                
                byte[] input;
                
                try {
                    while ((input = queue.pollFirst()) != null) {
                        raw.put(shouldEncrypt ? encryption.doFinal(input) : input);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("An exception occurred whilst encrypting data:", e);
                }
                
                raw.flip();
                
                queue.addAll(packet.getQueue());
                
                // It is important to synchronize on the packetsToFlush here, as we don't want the callback to
                // complete before the packet is queued.
                synchronized (packetsToFlush) {
                    if (!writing.getAndSet(true)) {
                        channel.write(raw, new Tuple<>(this, raw), PACKET_HANDLER);
                    } else {
                        packetsToFlush.offerFirst(raw);
                    }
                }

                totalBytes = packet.getSize(this);
            }
        }
    }

    /**
     * Gets the {@link Queue} that manages outgoing {@link Packet}s before writing them to the {@link Channel}.
     * <br><br>
     * This method should only be used internally; modifying this queue in any way can produce unintended results!
     *
     * @return A {@link Queue}.
     */
    public final Queue<Packet> getOutgoingPackets() {
        return outgoingPackets;
    }

    /**
     * Gets the backing {@link Channel} of this {@link Client}.
     *
     * @return This {@link Client}'s backing channel.
     */
    @Override
    public final AsynchronousSocketChannel getChannel() {
        return channel;
    }
    
    /**
     * Gets the encryption {@link Cipher} used by this {@link Client}.
     *
     * @return This {@link Client}'s encryption {@link Cipher}; possibly {@code null} if not yet set.
     */
    public final Cipher getEncryption() {
        return encryption;
    }
    
    /**
     * Gets the decryption {@link Cipher} used by this {@link Client}.
     *
     * @return This {@link Client}'s decryption {@link Cipher}; possibly {@code null} if not yet set.
     */
    public final Cipher getDecryption() {
        return decryption;
    }
    
    /**
     * Sets the encryption {@link Cipher} used by this {@link Client}.
     *
     * @param encryption The {@link Cipher} to set this {@link Client}'s encryption {@link Cipher} to.
     */
    public final void setEncryption(Cipher encryption) {
        this.encryption = encryption;
    }
    
    /**
     * Sets the decryption {@link Cipher} used by this {@link Client}.
     *
     * @param decryption The {@link Cipher} to set this {@link Client}'s decryption {@link Cipher} to.
     */
    public final void setDecryption(Cipher decryption) {
        this.decryption = decryption;
    }

}
