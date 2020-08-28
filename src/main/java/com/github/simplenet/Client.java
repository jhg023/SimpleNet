/*
 * MIT License
 *
 * Copyright (c) 2020 Jacob Glickman
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
package com.github.simplenet;

import com.github.pbbl.AbstractBufferPool;
import com.github.pbbl.direct.DirectByteBufferPool;
import com.github.simplenet.packet.Packet;
import com.github.simplenet.utility.IntPair;
import com.github.simplenet.utility.MutableBoolean;
import com.github.simplenet.utility.Pair;
import com.github.simplenet.utility.Utility;
import com.github.simplenet.utility.exposed.cryptography.CryptographicFunction;
import com.github.simplenet.utility.exposed.data.BooleanReader;
import com.github.simplenet.utility.exposed.data.ByteReader;
import com.github.simplenet.utility.exposed.data.CharReader;
import com.github.simplenet.utility.exposed.data.DoubleReader;
import com.github.simplenet.utility.exposed.data.FloatReader;
import com.github.simplenet.utility.exposed.data.IntReader;
import com.github.simplenet.utility.exposed.data.LongReader;
import com.github.simplenet.utility.exposed.data.StringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
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
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The entity that will connect to the {@link Server}.
 *
 * @author Jacob G.
 * @since November 1, 2017
 */
public class Client extends AbstractReceiver<Runnable> implements Channeled<AsynchronousSocketChannel>, BooleanReader,
        ByteReader, CharReader, IntReader, FloatReader, LongReader, DoubleReader, StringReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    /**
     * The {@link CompletionHandler} used to process bytes when they are received by this {@link Client}.
     */
    static class Listener implements CompletionHandler<Integer, Pair<Client, ByteBuffer>> {

        /**
         * A {@code static} instance of this class to be reused.
         */
        static final Listener INSTANCE = new Listener();
        
        @Override
        public void completed(Integer result, Pair<Client, ByteBuffer> pair) {
            // A result of -1 normally means that the end-of-stream has been reached. In that case, close the
            // client's connection.
            int bytesReceived = result;

            if (bytesReceived == -1) {
                pair.getKey().close(false);
                return;
            }

            var client = pair.getKey();
            var buffer = pair.getValue().flip();

            synchronized (client.queue) {
                var queue = client.queue;

                IntPair<Predicate<ByteBuffer>> peek;

                if ((peek = queue.peekLast()) == null) {
                    client.readInProgress.set(false);
                    return;
                }

                var stack = client.stack;

                boolean shouldDecrypt = client.decryptionCipher != null;
                boolean queueIsEmpty = false;

                int key;

                client.inCallback.set(true);

                while (buffer.remaining() >= (key = peek.getKey())) {
                    var wrappedBuffer = buffer.duplicate().mark().limit(buffer.position() + key);

                    if (shouldDecrypt) {
                        try {
                            wrappedBuffer = client.decryptionFunction.apply(client.decryptionCipher, wrappedBuffer)
                                    .reset();
                        } catch (Exception e) {
                            throw new IllegalStateException("An exception occurred whilst encrypting data:", e);
                        }
                    }

                    // If the predicate returns false, poll the element from the queue.
                    if (!peek.getValue().test(wrappedBuffer)) {
                        queue.pollLast();
                    }

                    if (wrappedBuffer.hasRemaining()) {
                        int remaining = wrappedBuffer.remaining();
                        byte[] decodedData = new byte[Math.min(key, 8)];
                        wrappedBuffer.reset().get(decodedData);
                        LOGGER.warn("A packet has not been read fully! {} byte(s) leftover! First 8 bytes of data: {}",
                                remaining, decodedData);
                    }

                    buffer.position(wrappedBuffer.limit());

                    while (!stack.isEmpty()) {
                        queue.offerLast(stack.pop());
                    }

                    if ((peek = queue.peekLast()) == null) {
                        queueIsEmpty = true;
                        break;
                    }
                }

                client.inCallback.set(false);

                // If the queue is not empty and there exists remaining data in the buffer, then that means
                // that we haven't received all of the requested data, and must re-use the same buffer.
                if (!queueIsEmpty && buffer.hasRemaining()) {
                    client.channel.read(buffer.position(buffer.limit()).limit(key), pair, this);
                } else {
                    // The buffer that was used must be returned to the pool.
                    DIRECT_BUFFER_POOL.give(buffer);

                    if (queueIsEmpty) {
                        // Because the queue is empty, the client should not attempt to read more data until
                        // more is requested by the user.
                        client.readInProgress.set(false);
                    } else {
                        // Because the queue is NOT empty and we don't have enough data to process the request,
                        // we must read more data.
                        var newBuffer = DIRECT_BUFFER_POOL.take(peek.getKey());
                        client.channel.read(newBuffer, new Pair<>(client, newBuffer), this);
                    }
                }
            }
        }

        @Override
        public void failed(Throwable t, Pair<Client, ByteBuffer> pair) {
            pair.getKey().close(false);
        }
    }

    /**
     * The {@link CompletionHandler} used when this {@link Client} sends one or more {@link Packet}s to a
     * {@link Server}.
     */
    private final CompletionHandler<Integer, ByteBuffer> packetHandler = new CompletionHandler<>() {
        @Override
        public void completed(Integer result, ByteBuffer buffer) {
            Client client = Client.this;
    
            DIRECT_BUFFER_POOL.give(buffer);

            synchronized (client.outgoingPackets) {
                ByteBuffer payload = client.packetsToFlush.poll();
    
                if (payload == null) {
                    client.writeInProgress.set(false);
                    return;
                }

                client.channel.write(payload, payload, this);
            }
        }

        @Override
        public void failed(Throwable t, ByteBuffer buffer) {
            Client client = Client.this;

            DIRECT_BUFFER_POOL.give(buffer);

            synchronized (client.outgoingPackets) {
                ByteBuffer discard;

                while ((discard = client.packetsToFlush.poll()) != null) {
                    DIRECT_BUFFER_POOL.give(discard);
                }
            }

            client.writeInProgress.set(false);
        }
    };

    /**
     * An {@link AbstractBufferPool<ByteBuffer>} that dispatches reusable, direct {@code ByteBuffer} objects.
     */
    private static final AbstractBufferPool<ByteBuffer> DIRECT_BUFFER_POOL = new DirectByteBufferPool();

    /**
     * A {@link MutableBoolean} that keeps track of whether or not the executing code is inside a callback.
     */
    private final MutableBoolean inCallback;
    
    /**
     * A thread-safe method of keeping track if this {@link Client} is in the process of shutting down.
     */
    private final AtomicBoolean closing;
    
    /**
     * A thread-safe method of keeping track if this {@link Client} is currently waiting for bytes to arrive.
     */
    private final AtomicBoolean readInProgress;
    
    /**
     * A thread-safe method of keeping track whether this {@link Client} is currently writing data to the network.
     */
    private final AtomicBoolean writeInProgress;
    
    /**
     * A {@link Queue} to manage outgoing {@link Packet}s.
     */
    private final Queue<Packet> outgoingPackets;

    /**
     * A {@link Deque} to manage {@link Packet}s that should be flushed as soon as possible.
     */
    private final Queue<ByteBuffer> packetsToFlush;

    /**
     * The {@link Deque} that keeps track of nested calls to {@link Client#readUntil(int, Predicate, ByteOrder)} and
     * assures that they will complete in the expected order.
     */
    private final Deque<IntPair<Predicate<ByteBuffer>>> stack;

    /**
     * The {@link Deque} used when requesting a certain amount of bytes from the {@link Client} or {@link Server}.
     */
    private final Deque<IntPair<Predicate<ByteBuffer>>> queue;

    /**
     * Whether or not the decryption {@link Cipher} specifies {@code NoPadding} as part of its algorithm.
     */
    private boolean decryptionNoPadding;

    /**
     * Whether or not the encryption {@link Cipher} specifies {@code NoPadding} as part of its algorithm.
     */
    private boolean encryptionNoPadding;

    /**
     * The {@link Cipher} used for {@link Packet} decryptionCipher.
     */
    private Cipher decryptionCipher;

    /**
     * The {@link Cipher} used for {@link Packet} encryptionCipher.
     */
    private Cipher encryptionCipher;

    /**
     * The {@link CryptographicFunction} responsible for decrypting data sent to a {@link Client}, which defaults to
     * {@link Cipher#doFinal(byte[])}.
     */
    private CryptographicFunction decryptionFunction;

    /**
     * The {@link CryptographicFunction} responsible for encrypting data sent to a {@link Client}, which defaults to
     * {@link Cipher#doFinal(byte[])}.
     */
    private CryptographicFunction encryptionFunction;

    /**
     * The backing {@link AsynchronousChannelGroup} of this {@link Client}.
     */
    private AsynchronousChannelGroup group;
    
    /**
     * The backing {@link Channel} of a {@link Client}.
     */
    private AsynchronousSocketChannel channel;
    
    /**
     * Instantiates a new {@link Client}.
     */
    public Client() {
        this((AsynchronousSocketChannel) null);
    }

    /**
     * Instantiates a new {@link Client} with an existing {@link AsynchronousSocketChannel}.
     *
     * @param channel The channel to back this {@link Client} with.
     */
    Client(AsynchronousSocketChannel channel) {
        closing = new AtomicBoolean();
        inCallback = new MutableBoolean();
        readInProgress = new AtomicBoolean();
        writeInProgress = new AtomicBoolean();
        outgoingPackets = new ArrayDeque<>();
        packetsToFlush = new ArrayDeque<>();
        queue = new ArrayDeque<>();
        stack = new ArrayDeque<>();
        
        if (channel != null) {
            this.channel = channel;
        }
    }
    
    /**
     * Instantiates a new {@link Client} (whose fields directly refer to the fields of the specified {@link Client})
     * from an existing {@link Client}, essentially acting as a shallow copy-constructor.
     * <br><br>
     * This exists so that, if a user creates a class that extends {@link Client}, they can pass in an existing
     * {@link Client}, allowing them to invoke {@code super(client)} inside their constructor. Doing so will allow
     * them to invoke {@code wrapper.readByte()} (for example) instead of {@code wrapper.getClient().readByte()}.
     *
     * @param client An existing {@link Client} whose backing {@link AsynchronousSocketChannel} is already connected.
     */
    protected Client(Client client) {
        super(client);

        this.stack = client.stack;
        this.queue = client.queue;
        this.channel = client.channel;
        this.closing = client.closing;
        this.inCallback = client.inCallback;
        this.packetsToFlush = client.packetsToFlush;
        this.readInProgress = client.readInProgress;
        this.writeInProgress = client.writeInProgress;
        this.outgoingPackets = client.outgoingPackets;
        this.encryptionCipher = client.encryptionCipher;
        this.decryptionCipher = client.decryptionCipher;
        this.encryptionFunction = client.encryptionFunction;
        this.decryptionFunction = client.decryptionFunction;
        this.decryptionNoPadding = client.decryptionNoPadding;
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
            LOGGER.warn("Couldn't connect to the server! Maybe it's offline?"));
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

        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("The specified port must be between 0 and 65535!");
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(false);
            thread.setName(thread.getName().replace("Thread", "SimpleNet"));
            
            return thread;
        }, (runnable, threadPoolExecutor) -> {});

        // Start one core thread in advance to prevent the JVM from shutting down.
        executor.prestartCoreThread();

        try {
            this.channel = AsynchronousSocketChannel.open(group = AsynchronousChannelGroup.withThreadPool(executor));
            this.channel.setOption(StandardSocketOptions.SO_RCVBUF, BUFFER_SIZE);
            this.channel.setOption(StandardSocketOptions.SO_SNDBUF, BUFFER_SIZE);
            this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
            this.channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open the channel!", e);
        }

        try {
            channel.connect(new InetSocketAddress(address, port)).get(timeout, unit);
        } catch (AlreadyConnectedException e) {
            throw new IllegalStateException("This client is already connected to a server!", e);
        } catch (Exception e) {
            onTimeout.run();
            close(false);
            return;
        }
        
        executor.execute(() -> connectListeners.forEach(Runnable::run));
    }

    /**
     * The implementation of {@link #close()} that may or may not wait for flushed packets to be written
     * successfully, depending on the value of {@code waitForWrite}.
     *
     * @param waitForWrite Whether or not to wait for flushed packets to be written successfully.
     */
    private void close(boolean waitForWrite) {
        // If this Client is already closing, do nothing.
        if (closing.getAndSet(true)) {
            return;
        }

        preDisconnectListeners.forEach(Runnable::run);

        if (waitForWrite) {
            flush();

            while (writeInProgress.get()) {
                Thread.onSpinWait();
            }
        }

        Channeled.super.close();

        while (channel.isOpen()) {
            Thread.onSpinWait();
        }

        postDisconnectListeners.forEach(Runnable::run);

        if (group != null) {
            try {
                group.shutdownNow();
            } catch (IOException e) {
                LOGGER.debug("An IOException occurred when shutting down the AsynchronousChannelGroup!", e);
            }
        }
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
        close(true);
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
    public void readUntil(int n, Predicate<ByteBuffer> predicate, ByteOrder order) {
        boolean shouldDecrypt = decryptionCipher != null;
    
        if (shouldDecrypt && !decryptionNoPadding) {
            int blockSize = decryptionCipher.getBlockSize();
            n = Utility.roundUpToNextMultiple(n, blockSize == 0 ? decryptionCipher.getOutputSize(n) : blockSize);
        }

        var pair = new IntPair<Predicate<ByteBuffer>>(n, buffer -> predicate.test(buffer.order(order)));

        synchronized (queue) {
            if (inCallback.get()) {
                stack.push(pair);
                return;
            }

            queue.offerFirst(pair);

            if (!readInProgress.getAndSet(true)) {
                var buffer = DIRECT_BUFFER_POOL.take(n);
                channel.read(buffer, new Pair<>(this, buffer), Listener.INSTANCE);
            }
        }
    }

    /**
     * Flushes any queued {@link Packet}s held within the internal {@link Queue}.
     * <br><br>
     * Any {@link Packet}s queued after the call to this method will not be flushed until this method is called again.
     */
    public final void flush() {
        Packet packet;

        boolean shouldEncrypt = encryptionCipher != null;

        Deque<Consumer<ByteBuffer>> queue;

        synchronized (outgoingPackets) {
            while ((packet = outgoingPackets.poll()) != null) {
                queue = packet.getQueue();

                ByteBuffer raw = DIRECT_BUFFER_POOL.take(packet.getSize(this));

                for (var input : queue) {
                    input.accept(raw);
                }

                if (shouldEncrypt) {
                    try {
                        raw = encryptionFunction.apply(encryptionCipher, raw.flip());
                    } catch (GeneralSecurityException e) {
                        throw new IllegalStateException("An exception occurred whilst encrypting data!", e);
                    }
                }

                raw.flip();

                if (!writeInProgress.getAndSet(true)) {
                    channel.write(raw, raw, packetHandler);
                } else {
                    packetsToFlush.offer(raw);
                }
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
    public final Cipher getEncryptionCipher() {
        return encryptionCipher;
    }
    
    /**
     * Gets the decryption {@link Cipher} used by this {@link Client}.
     *
     * @return This {@link Client}'s decryption {@link Cipher}; possibly {@code null} if not yet set.
     */
    public final Cipher getDecryptionCipher() {
        return decryptionCipher;
    }
    
    /**
     * Sets the encryption {@link Cipher} used by this {@link Client}.
     * <br><br>
     * After calling this method, data being sent will automatically be encrypted using {@link Cipher#doFinal(byte[])}.
     *
     * @param encryptionCipher The {@link Cipher} to set this {@link Client}'s encryption {@link Cipher} to.
     */
    public final void setEncryptionCipher(Cipher encryptionCipher) {
        setEncryption(encryptionCipher, CryptographicFunction.DO_FINAL);
    }
    
    /**
     * Sets the encryption {@link Cipher} and {@link CryptographicFunction} used by this {@link Client}.
     *
     * @param encryptionCipher   The {@link Cipher} to set this {@link Client}'s encryption {@link Cipher} to.
     * @param encryptionFunction The {@link CryptographicFunction} responsible for the encryption of outgoing data.
     */
    public final void setEncryption(Cipher encryptionCipher, CryptographicFunction encryptionFunction) {
        this.encryptionCipher = encryptionCipher;
        this.encryptionFunction = encryptionFunction;
        this.encryptionNoPadding = encryptionCipher.getAlgorithm().endsWith("NoPadding");
    }

    /**
     * Gets whether or not this {@link Client}'s encryption {@link Cipher} specifies a {@code NoPadding} algorithm.
     *
     * @return {@code true} if the encryption algorithm being used specifies {@code NoPadding}, otherwise {@code false}.
     */
    public boolean isEncryptionNoPadding() {
        return encryptionNoPadding;
    }

    /**
     * Sets the decryption {@link Cipher} used by this {@link Client}.
     * <br><br>
     * After calling this method, data being received will automatically be decrypted using
     * {@link Cipher#doFinal(byte[])}.
     *
     * @param decryptionCipher The {@link Cipher} to set this {@link Client}'s decryption {@link Cipher} to.
     */
    public final void setDecryptionCipher(Cipher decryptionCipher) {
        setDecryption(decryptionCipher, CryptographicFunction.DO_FINAL);
    }
    
    /**
     * Sets the decryption {@link Cipher} and {@link CryptographicFunction} used by this {@link Client}.
     *
     * @param decryptionCipher   The {@link Cipher} to set this {@link Client}'s decryption {@link Cipher} to.
     * @param decryptionFunction The {@link CryptographicFunction} responsible for the decryption of incoming data.
     */
    public final void setDecryption(Cipher decryptionCipher, CryptographicFunction decryptionFunction) {
        this.decryptionCipher = decryptionCipher;
        this.decryptionFunction = decryptionFunction;
        this.decryptionNoPadding = decryptionCipher.getAlgorithm().endsWith("NoPadding");
    }
}
