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
package com.github.simplenet;

import com.github.simplenet.channel.Channeled;
import com.github.simplenet.packet.Packet;
import com.github.simplenet.utility.IntPair;
import com.github.simplenet.utility.MutableBoolean;
import com.github.simplenet.utility.MutableInt;
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
import pbbl.ByteBufferPool;
import pbbl.direct.DirectByteBufferPool;

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
public class Client extends Receiver<Runnable> implements Channeled<AsynchronousSocketChannel>, BooleanReader,
        ByteReader, CharReader, IntReader, FloatReader, LongReader, DoubleReader, StringReader {
    
    /**
     * The {@link CompletionHandler} used to process bytes when they are received by this {@link Client}.
     */
    static class Listener implements CompletionHandler<Integer, Client> {

        /**
         * A {@code static} instance of this class to be reused.
         */
        static final Listener INSTANCE = new Listener();
        
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
                
                IntPair<Predicate<ByteBuffer>> peek;
    
                if ((peek = queue.peekLast()) == null) {
                    client.readInProgress.set(false);
					return;
                }

                var stack = client.stack;

                boolean shouldDecrypt = client.decryptionCipher != null;
				boolean isQueueEmpty = false;

				int key;
	
				client.inCallback.set(true);
                
                while (client.size.get() >= (key = peek.key)) {
                    client.size.add(-key);

                    byte[] data = new byte[key];

                    buffer.get(data);

                    ByteBuffer wrappedBuffer = ByteBuffer.wrap(data);

                    if (shouldDecrypt) {
                        try {
                            wrappedBuffer = client.decryptionFunction.apply(client.decryptionCipher,
                                    wrappedBuffer).flip();
                        } catch (Exception e) {
                            throw new IllegalStateException("An exception occurred whilst encrypting data:", e);
                        }
                    }

                    // If the predicate returns false, poll the element from the queue.
                    if (!peek.value.test(wrappedBuffer)) {
                        queue.pollLast();
                    }
    
                    if (wrappedBuffer.hasRemaining()) {
                        if (Utility.isDebug()) {
                            System.err.println(wrappedBuffer.remaining() + " byte(s) still need to be read!");
                        }
                    }
                    
                    while (!stack.isEmpty()) {
                        queue.offerLast(stack.pop());
                    }
        
                    if ((peek = queue.peekLast()) == null) {
                    	isQueueEmpty = true;
                        break;
                    }
                }
    
                client.inCallback.set(false);
                
                if (client.size.get() > 0) {
                    buffer.compact();
                } else {
                    buffer.clear();
                }
                
                if (isQueueEmpty) {
					// Because the queue is empty, the client should not attempt to read more data until
					// more is requested by the user.
                    buffer.position(0);
					client.readInProgress.set(false);
				} else {
					// Because the queue is NOT empty and we don't have enough data to process the request,
					// we must read more data.
					client.channel.read(buffer, client, this);
				}
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

    private static final CryptographicFunction DO_FINAL = (cipher, data) -> {
        ByteBuffer output = data.duplicate().limit(cipher.getOutputSize(data.limit()));
        cipher.doFinal(data, output);
        return output;
    };

    /**
     * A {@link ByteBufferPool} that dispatches reusable {@code DirectByteBuffer}s.
     */
    private static final ByteBufferPool DIRECT_BUFFER_POOL = new DirectByteBufferPool();
    
    /**
     * The {@link ByteBuffer} that will hold data sent by the {@link Client} or {@link Server}.
     */
    private final ByteBuffer buffer;
    
    /**
     * The amount of readable bytes that currently exist within this {@link Client}'s {@code buffer}.
     * <br><br>
     * This is a {@link MutableInt} because, if it were an {@code int}, then the copy constructor of {@link Client}
     * wouldn't reference the same value.
     */
    private final MutableInt size;
	
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
     * A {@link Deque} to manage outgoing {@link Packet}s.
     */
    private final Deque<Packet> outgoingPackets;

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
     * Instantiates a new {@link Client} by attempting to open the backing {@link AsynchronousSocketChannel} with a
     * default buffer size of {@code 4,096} bytes.
     */
    public Client() {
        this(8_192);
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
        closing = new AtomicBoolean();
        inCallback = new MutableBoolean();
        readInProgress = new AtomicBoolean();
        writeInProgress = new AtomicBoolean();
        outgoingPackets = new ArrayDeque<>();
        packetsToFlush = new ArrayDeque<>();
        queue = new ArrayDeque<>();
        stack = new ArrayDeque<>();
        buffer = DIRECT_BUFFER_POOL.take(bufferSize);
        
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
	
		this.size = client.size;
		this.stack = client.stack;
		this.queue = client.queue;
        this.buffer = client.buffer;
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

        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(false);
            thread.setName(thread.getName().replace("Thread", "SimpleNet"));
            
            if (Utility.isDebug()) {
                thread.setUncaughtExceptionHandler(($, throwable) -> throwable.printStackTrace());
            }
            
            return thread;
        }, (runnable, threadPoolExecutor) -> {});

        executor.prestartAllCoreThreads();

        try {
            this.channel = AsynchronousSocketChannel.open(group = AsynchronousChannelGroup.withThreadPool(executor));
            this.channel.setOption(StandardSocketOptions.SO_RCVBUF, bufferSize);
            this.channel.setOption(StandardSocketOptions.SO_SNDBUF, bufferSize);
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
            close();
            return;
        }
        
        executor.execute(() -> connectListeners.forEach(Runnable::run));
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
        // If this Client is already closing, do nothing.
        if (closing.getAndSet(true)) {
            return;
        }

        preDisconnectListeners.forEach(Runnable::run);

        flush();

        while (channel.isOpen() && writeInProgress.get()) {
            Thread.onSpinWait();
        }

        if (channel.isOpen()) {
            Channeled.super.close();

            while (channel.isOpen()) {
                Thread.onSpinWait();
            }
        }

        postDisconnectListeners.forEach(Runnable::run);

        if (group != null) {
            try {
                group.shutdownNow();
            } catch (IOException e) {
                if (Utility.isDebug()) {
                    e.printStackTrace();
                }
            }
        }

        DIRECT_BUFFER_POOL.give(buffer);
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
            n = Utility.roundUpToNextMultiple(n, decryptionCipher.getBlockSize());
        }
	
        synchronized (buffer) {
            IntPair<Predicate<ByteBuffer>> pair = new IntPair<>(n, buffer -> predicate.test(buffer.order(order)));
            
            if (inCallback.get()) {
                stack.push(pair);
                return;
            }
	
			while (size.get() >= n && queue.isEmpty() && stack.isEmpty()) {
				size.add(-n);

				var data = new byte[n];

				buffer.get(data);

                var wrappedBuffer = ByteBuffer.wrap(data);

				if (shouldDecrypt) {
					try {
						wrappedBuffer = decryptionFunction.apply(decryptionCipher, wrappedBuffer);
					} catch (GeneralSecurityException e) {
						throw new IllegalStateException("An exception occurred whilst decrypting data!", e);
					}
				}
		
				boolean shouldReturn = !predicate.test(wrappedBuffer);
                
                if (wrappedBuffer.hasRemaining()) {
                    if (Utility.isDebug()) {
                        System.err.println(wrappedBuffer.remaining() + " byte(s) still need to be read!");
                    }
                }
				
				if (shouldReturn) {
					return;
				}
			}
	
			queue.offerFirst(pair);
	
			if (!readInProgress.getAndSet(true)) {
				channel.read(buffer.position(size.get()), this, Listener.INSTANCE);
			}
        }
    }

    /**
     * Flushes any queued {@link Packet}s held within the internal {@link Queue}.
     * <br><br>
     * Any {@link Packet}s queued after the call to this method will not be flushed until this method is called again.
     */
    public final void flush() {
        Packet packet, overflow = null;

        int totalBytes = 0;

        boolean shouldEncrypt = encryptionCipher != null;

        var queue = new ArrayDeque<Consumer<ByteBuffer>>();

        synchronized (outgoingPackets) {
            while ((packet = outgoingPackets.pollLast()) != null) {
                int currentBytes = totalBytes;
                boolean isTooBig = (totalBytes += packet.getSize(this)) > bufferSize;
                boolean isEmpty = outgoingPackets.isEmpty();

                if (isTooBig) {
                    overflow = packet;
                } else {
                    queue.addAll(packet.getQueue());
                }

                if (isTooBig || isEmpty) {
                    ByteBuffer raw = DIRECT_BUFFER_POOL.take(isEmpty ? totalBytes : currentBytes);

                    Consumer<ByteBuffer> input;

                    while ((input = queue.pollFirst()) != null) {
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
                        try {
                            channel.write(raw, raw, packetHandler);
                        } catch (Exception e) {
                            // TODO: Remove stack trace printing if this works
                            e.printStackTrace();
                            writeInProgress.set(false);
                            return;
                        }
                    } else {
                        packetsToFlush.offer(raw);
                    }
                }

                // If there exists a packet which could not be flushed due to overflow, add it to the queue to be
                // flushed on the next iteration, and update 'totalBytes'.
                if (overflow != null) {
                    outgoingPackets.offerLast(overflow);
                    totalBytes = 0;
                    overflow = null;
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
    public final Deque<Packet> getOutgoingPackets() {
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
        setEncryption(encryptionCipher, DO_FINAL);
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
        setDecryption(decryptionCipher, DO_FINAL);
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
