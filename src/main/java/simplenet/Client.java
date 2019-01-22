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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import simplenet.channel.Channeled;
import simplenet.packet.Packet;
import simplenet.receiver.Receiver;
import simplenet.utility.IntPair;
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
                if (t.getMessage().startsWith("The specified network name is no longer available.")) {
                    client.close();
                }
            }
        };
        
        @Override
        public void completed(Integer result, Client client) {
            // A result of -1 normally means that the end-of-stream has been reached. In that case, close the
            // client's connection.
            if (result == -1) {
                client.close();
                return;
            }
            
            synchronized (client.buffer) {
                client.size += result;
                
                var buffer = client.buffer.flip();
                var queue = client.queue;
    
                IntPair<Consumer<ByteBuffer>> peek;
    
                if ((peek = queue.pollLast()) == null) {
                    client.channel.read(buffer.flip().limit(buffer.capacity()), client, this);
                    return;
                }
    
                client.prepend = true;
    
                boolean decrypt = client.decryption != null;
    
                var stack = client.stack;
    
                int key;
    
                while (client.size >= (key = peek.getKey())) {
                    if (decrypt) {
                        try {
                            int position = buffer.position();
                            buffer.limit(buffer.position() + key);
                            client.decryption.update(buffer, buffer.duplicate());
                            buffer.flip().position(position);
                        } catch (Exception e) {
                            throw new IllegalStateException("Exception occurred when decrypting:", e);
                        }
                    }
        
                    client.size -= key;
                    
                    peek.getValue().accept(buffer);
        
                    while (!stack.isEmpty()) {
                        queue.offerFirst(stack.pollFirst());
                    }
        
                    if ((peek = queue.pollLast()) == null) {
                        break;
                    }
                }
    
                client.prepend = false;
    
                if (peek != null) {
                    queue.addLast(peek);
                }
    
                if (client.size > 0) {
                    buffer.compact();
                } else {
                    buffer.flip();
                }
                
                client.channel.read(buffer.limit(buffer.capacity()), client, this);
            }
        }

        @Override
        public void failed(Throwable t, Client client) {
            client.close();
        }
    }

    /**
     * The {@link CompletionHandler} used when this {@link Client} sends one or more {@link Packet}s
     * to a {@link Server}.
     */
    private static final CompletionHandler<Integer, Client> PACKET_HANDLER = new CompletionHandler<>() {
        @Override
        public void completed(Integer result, Client client) {
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

            client.channel.write(payload, client, this);
        }

        @Override
        public void failed(Throwable t, Client client) {
            t.printStackTrace();
        }
    };

    /**
     * A thread-safe method of keeping track whether this {@link Client} is currently writing data
     * to the network.
     */
    private final AtomicBoolean writing;
    
    /**
     * The {@link ByteBuffer} that will hold data sent by the {@link Client} or {@link Server}.
     */
    private final ByteBuffer buffer;

    /**
     * A {@link Queue} to manage outgoing {@link Packet}s.
     */
    private final Queue<Packet> outgoingPackets;

    /**
     * A {@link Deque} to manage {@link Packet}s that should be flushed as soon as possible.
     */
    private final Deque<ByteBuffer> packetsToFlush;

    /**
     * The {@link Deque} that keeps track of nested calls to {@link Client#read(int, Consumer)}
     * and assures that they will complete in the expected order.
     */
    private final Deque<IntPair<Consumer<ByteBuffer>>> stack;

    /**
     * The {@link Deque} used when requesting a certain amount of bytes from the {@link Client} or
     * {@link Server}.
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
     * The backing {@link ThreadPoolExecutor} used for I/O.
     */
    private ThreadPoolExecutor executor;

    /**
     * The backing {@link Channel} of a {@link Client}.
     */
    private AsynchronousSocketChannel channel;
    
    /**
     * The amount of readable {@code byte}s that currently exist within this {@link Client}'s {@code buffer}.
     */
    private volatile int size;
    
    /**
     * Instantiates a new {@link Client} by attempting to open the backing
     * {@link AsynchronousSocketChannel} with a default buffer size of {@code 4096} bytes.
     */
    public Client() {
        this(4096);
    }

    /**
     * Instantiates a new {@link Client} by attempting to open the backing {@link AsynchronousSocketChannel}
     * with a provided buffer size in bytes.
     *
     * @param bufferSize The size of this {@link Client}'s buffer, in bytes.
     */
    public Client(int bufferSize) {
        this(bufferSize, null);
    }

    /**
     * Instantiates a new {@link Client} with an existing {@link AsynchronousSocketChannel} with a provided
     * buffer size in bytes.
     *
     * @param bufferSize The size of this {@link Client}'s buffer, in bytes.
     * @param channel    The channel to back this {@link Client} with.
     */
    public Client(int bufferSize, AsynchronousSocketChannel channel) {
        super(bufferSize);
        
        writing = new AtomicBoolean();
        outgoingPackets = new ConcurrentLinkedDeque<>();
        packetsToFlush = new ConcurrentLinkedDeque<>();
        queue = new ConcurrentLinkedDeque<>();
        stack = new ConcurrentLinkedDeque<>();
        buffer = ByteBuffer.allocateDirect(bufferSize);
        
        if (channel != null) {
            this.channel = channel;
        }
    }

    /**
     * Attempts to connect to a {@link Server} with the specified {@code address} and {@code port}
     * and a default timeout of {@code 30} seconds.
     *
     * @param address The IP address to connect to.
     * @param port    The port to connect to {@code 0 <= port <= 65535}.
     * @throws IllegalArgumentException  If {@code port} is less than 0 or greater than 65535.
     * @throws AlreadyConnectedException If a {@link Client} is already connected to any address/port.
     */
    public final void connect(String address, int port) {
        connect(address, port, 30L, TimeUnit.SECONDS, () -> {
            System.err.println("Couldn't connect within 30 seconds!");
        });
    }

    /**
     * Attempts to connect to a {@link Server} with the specified {@code address} and {@code port}
     * and a specified timeout.  If the timeout is reached, then the {@link Runnable} is run and
     * the backing {@link AsynchronousSocketChannel} is closed.
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
            throw new IllegalArgumentException("The port must be between 0 and 65535!");
        }

        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(false);
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
        } catch (ExecutionException e) {
            throw new IllegalStateException("An ExecutionException has occurred:", e);
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
    public void close() {
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
    public void preDisconnect(Runnable listener) {
        preDisconnectListeners.add(listener);
    }

    /**
     * Registers a listener that fires right after a {@link Client} disconnects from a {@link Server}.
     * <br><br>
     * Calling this method more than once registers multiple listeners.
     *
     * @param listener A {@link Runnable}.
     */
    public void postDisconnect(Runnable listener) {
        postDisconnectListeners.add(listener);
    }
    
    @Override
    public void read(int n, Consumer<ByteBuffer> consumer, ByteOrder order) {
        synchronized (buffer) {
            if (size >= n) {
                size -= n;
                
                consumer.accept(buffer.order(order));
                return;
            }
        
            if (prepend) {
                stack.offerLast(new IntPair<>(n, buffer -> consumer.accept(buffer.order(order))));
            } else {
                queue.offerFirst(new IntPair<>(n, buffer -> consumer.accept(buffer.order(order))));
            }
        }
    }

    /**
     * Flushes any queued {@link Packet}s held within the internal {@link Queue}.
     * <br><br>
     * Any {@link Packet}s queued after the call to this method will not be flushed until
     * this method is called again.
     */
    public final void flush() {
        int totalBytes = 0;
        int amountToPoll = outgoingPackets.size();
        
        Packet packet;

        var queue = new ArrayDeque<Consumer<ByteBuffer>>();

        while (amountToPoll-- > 0 && (packet = outgoingPackets.poll()) != null) {
            int currentBytes = totalBytes;

            boolean tooBig = (totalBytes += packet.getSize()) >= bufferSize;
            boolean empty = outgoingPackets.isEmpty();

            if (!tooBig || empty) {
                queue.addAll(packet.getQueue());
            }

            // If we've buffered all of the packets that we can, send them off.
            if (tooBig || empty) {
                var raw = ByteBuffer.allocateDirect(empty ? totalBytes : currentBytes);

                Consumer<ByteBuffer> consumer;

                while ((consumer = queue.pollFirst()) != null) {
                    consumer.accept(raw);
                }

                queue.addAll(packet.getQueue());

                raw.flip();

                if (encryption != null) {
                    try {
                        encryption.update(raw, raw.duplicate());
                        raw.flip();
                    } catch (Exception e) {
                        throw new IllegalStateException("Exception occurred when encrypting:", e);
                    }
                }

                if (!writing.getAndSet(true)) {
                    channel.write(raw, this, PACKET_HANDLER);
                } else {
                    packetsToFlush.offerFirst(raw);
                }

                totalBytes = packet.getSize();
            }
        }
    }

    /**
     * Gets the {@link Queue} that manages outgoing {@link Packet}s before writing them to the
     * {@link Channel}.
     *
     * @return A {@link Queue}.
     */
    public Queue<Packet> getOutgoingPackets() {
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
     * Gets the {@link ByteBuffer} of this {@link Client}.
     *
     * @return This {@link Client}'s buffer.
     */
    ByteBuffer getBuffer() {
        return buffer;
    }

    public void setEncryption(Cipher encryption) {
        Objects.requireNonNull(encryption);

        if (!encryption.getAlgorithm().endsWith("NoPadding")) {
            throw new IllegalArgumentException("The cipher cannot have any padding!");
        }

        this.encryption = encryption;
    }

    public void setDecryption(Cipher decryption) {
        Objects.requireNonNull(decryption);

        if (!decryption.getAlgorithm().endsWith("NoPadding")) {
            throw new IllegalArgumentException("The cipher cannot have any padding!");
        }

        this.decryption = decryption;
    }

}
