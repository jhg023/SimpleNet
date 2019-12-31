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

import com.github.simplenet.cryptography.CryptographicFunction;
import com.github.simplenet.utility.MutableBoolean;
import com.github.simplenet.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pbbl.ByteBufferPool;
import pbbl.direct.DirectByteBufferPool;

import javax.crypto.Cipher;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The entity that will connect to a {@link Server}.
 *
 * @author Jacob G.
 * @since November 1, 2017
 */
public class Client implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    /**
     * The maximum value for a port that a {@link Server server} can be bound to.
     */
    private static final int MAX_PORT = 65_535;

    /**
     * A {@link ByteBufferPool} that dispatches reusable {@code DirectByteBuffer}s.
     */
    private static final ByteBufferPool DIRECT_BUFFER_POOL = new DirectByteBufferPool();

    /**
     * The default {@link CryptographicFunction} to use if one isn't specified when requesting encryption/decryption.
     */
    private static final CryptographicFunction DO_FINAL = (Cipher cipher, ByteBuffer data) -> {
        ByteBuffer output = data.duplicate().limit(cipher.getOutputSize(data.limit()));
        cipher.doFinal(data, output);
        return output;
    };

    /**
     * A {@link Queue} to manage outgoing {@link Packet}s.
     */
    final Queue<Packet> outgoingPackets;

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
     * A collection of listeners that are fired right after this {@link Client client} disconnects from a
     * {@link Server server}.
     */
	private final Collection<Runnable> disconnectListeners;

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
     * The backing {@link Channel} of a {@link Client}.
     */
    private SocketChannel channel;
    
    /**
     * Instantiates a new {@link Client}.
     */
    public Client() {
        this((SocketChannel) null);
    }

    /**
     * Instantiates a new {@link Client} with an existing {@link AsynchronousSocketChannel}.
     *
     * @param channel The channel to back this {@link Client} with.
     */
    Client(SocketChannel channel) {
        closing = new AtomicBoolean();
        inCallback = new MutableBoolean();
        readInProgress = new AtomicBoolean();
        outgoingPackets = new ArrayDeque<>();
        disconnectListeners = new ArrayList<>(1);

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
		this.channel = client.channel;
        this.closing = client.closing;
        this.inCallback = client.inCallback;
        this.readInProgress = client.readInProgress;
        this.outgoingPackets = client.outgoingPackets;
        this.encryptionCipher = client.encryptionCipher;
		this.decryptionCipher = client.decryptionCipher;
        this.encryptionFunction = client.encryptionFunction;
        this.decryptionFunction = client.decryptionFunction;
        this.encryptionNoPadding = client.encryptionNoPadding;
        this.decryptionNoPadding = client.decryptionNoPadding;
        this.disconnectListeners = client.disconnectListeners;
    }

    /**
     * Attempts to connect to a {@link Server} with the specified {@code address} and {@code port}.
     *
     * @param address The IP address to connect to.
     * @param port    The port to connect to {@code 0 <= port <= 65535}.
     */
    public final void connect(String address, int port) {
        Objects.requireNonNull(address);

        if (port < 0 || port > MAX_PORT) {
            throw new IllegalArgumentException("The specified port must be between 0 and 65535!");
        }

        try {
            channel = SocketChannel.open(new InetSocketAddress(address, port));
        } catch (IOException e) {
            throw new UncheckedIOException("An IOException occurred when attempting to connect to a server with the " +
                "specified address and port!", e);
        }

        try {
            channel.configureBlocking(true);
        } catch (IOException e) {
            throw new UncheckedIOException("An IOException occurred when configuring the channel to block!", e);
        }
    }

    /**
     * Closes this {@link Client}'s backing {@link SocketChannel channel} after flushing any queued packets.
     * <br><br>
     * Any registered disconnect listeners are fired after the backing channel has closed successfully.
     * <br><br>
     * Listeners are fired in the same order that they were registered in.
     */
    @Override
    public final void close() {
        if (closing.getAndSet(true)) {
            return;
        }

        flush();

        try {
            channel.close();
        } catch (IOException e) {
            LOGGER.error("An IOException occurred when attempting to close the backing channel!", e);
        }

        while (channel.isOpen()) {
            Thread.onSpinWait();
        }

        disconnectListeners.forEach(Runnable::run);
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

                try {
                    channel.write(raw.flip());
                } catch (IOException e) {
                    LOGGER.error("An IOException occurred when attempting to send a packet!", e);
                }
            }
        }
    }

    private ByteBuffer readHelper(int numBytesToRead, ByteOrder byteOrder) {
        boolean shouldDecrypt = decryptionCipher != null;

        if (shouldDecrypt && !decryptionNoPadding) {
            numBytesToRead = Utility.roundUpToNextMultiple(numBytesToRead, decryptionCipher.getBlockSize());
        }

        var buffer = DIRECT_BUFFER_POOL.take(numBytesToRead).order(byteOrder);

        try {
            channel.read(buffer);
        } catch (IOException e) {
            throw new UncheckedIOException("An IOException occurred when reading a byte!", e);
        }

        if (shouldDecrypt) {
            try {
                buffer = decryptionFunction.apply(decryptionCipher, buffer.flip());
            } catch (GeneralSecurityException e) {
                LOGGER.error("A GeneralSecurityException occurred when decrypting a byte!", e);
            }
        }

        return buffer.flip();
    }


    /**
     * Reads the specified amount of bytes into a temporary, big endian {@link ByteBuffer}, which is then passed to the
     * specified {@link Consumer consumer} for processing.
     *
     * @param numBytesToRead The number of bytes to read.
     * @param consumer       The {@link Consumer consumer} to accept with the temporary {@link ByteBuffer}.
     */
    public final void read(int numBytesToRead, Consumer<ByteBuffer> consumer) {
        read(numBytesToRead, ByteOrder.BIG_ENDIAN, consumer);
    }

    /**
     * Reads the specified amount of bytes into a temporary {@link ByteBuffer}, which is then passed to the specified
     * {@link Consumer consumer} for processing.
     *
     * @param numBytesToRead The number of bytes to read.
     * @param byteOrder      The order of the bytes in the temporary {@link ByteBuffer}.
     * @param consumer       The {@link Consumer consumer} to accept with the temporary {@link ByteBuffer}.
     */
    public final void read(int numBytesToRead, ByteOrder byteOrder, Consumer<ByteBuffer> consumer) {
        var buffer = readHelper(numBytesToRead, byteOrder);
        consumer.accept(buffer);

        if (buffer.hasRemaining()) {
            int remaining = buffer.remaining();
            byte[] decodedData = new byte[Math.min(numBytesToRead, 8)];
            buffer.clear().get(decodedData);
            LOGGER.warn("A packet has not been read fully! {} byte(s) leftover! First 8 bytes of data: {}",
                remaining, Arrays.toString(decodedData));
        }

        DIRECT_BUFFER_POOL.give(buffer);
    }

    /**
     * Reads a {@code boolean} from the network, blocking until it is received.
     *
     * @return A {@code boolean}.
     */
    public final boolean readBoolean() {
        var buffer = readHelper(Byte.BYTES, ByteOrder.BIG_ENDIAN);
        byte value = buffer.get();
        DIRECT_BUFFER_POOL.give(buffer);
        return value == 1;
    }

    /**
     * Reads a {@code byte} from the network, blocking until it is received.
     *
     * @return A {@code byte}.
     */
    public final byte readByte() {
        var buffer = readHelper(Byte.BYTES, ByteOrder.BIG_ENDIAN);
        byte value = buffer.get();
        DIRECT_BUFFER_POOL.give(buffer);
        return value;
    }

    /**
     * Reads a {@code char} from the network, blocking until it is received.
     *
     * @return A big endian {@code char}.
     */
    public final char readChar() {
        return readChar(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a {@code char} from the network, blocking until it is received.
     *
     * @param byteOrder The order of the bytes in the {@code char}.
     * @return A {@code char} with the specified {@link ByteOrder byte order}.
     */
    public final char readChar(ByteOrder byteOrder) {
        var buffer = readHelper(Character.BYTES, byteOrder);
        char value = buffer.getChar();
        DIRECT_BUFFER_POOL.give(buffer);
        return value;
    }

    /**
     * Reads a {@code double} from the network, blocking until it is received.
     *
     * @return A big endian {@code double}.
     */
    public final double readDouble() {
        return readDouble(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a {@code double} from the network, blocking until it is received.
     *
     * @param byteOrder The order of the bytes in the {@code double}.
     * @return A {@code double} with the specified {@link ByteOrder byte order}.
     */
    public final double readDouble(ByteOrder byteOrder) {
        var buffer = readHelper(Double.BYTES, byteOrder);
        double value = buffer.getDouble();
        DIRECT_BUFFER_POOL.give(buffer);
        return value;
    }

    /**
     * Reads a {@code float} from the network, blocking until it is received.
     *
     * @return A big endian {@code float}.
     */
    public final float readFloat() {
        return readFloat(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a {@code float} from the network, blocking until it is received.
     *
     * @param byteOrder The order of the bytes in the {@code float}.
     * @return A {@code float} with the specified {@link ByteOrder byte order}.
     */
    public final float readFloat(ByteOrder byteOrder) {
        var buffer = readHelper(Float.BYTES, byteOrder);
        float value = buffer.getFloat();
        DIRECT_BUFFER_POOL.give(buffer);
        return value;
    }

    /**
     * Reads a {@code int} from the network, blocking until it is received.
     *
     * @return A big endian {@code int}.
     */
    public final int readInt() {
        return readInt(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads an {@code int} from the network, blocking until it is received.
     *
     * @param byteOrder The order of the bytes in the {@code int}.
     * @return An {@code int} with the specified {@link ByteOrder byte order}.
     */
    public final int readInt(ByteOrder byteOrder) {
        var buffer = readHelper(Integer.BYTES, byteOrder);
        int value = buffer.getInt();
        DIRECT_BUFFER_POOL.give(buffer);
        return value;
    }

    /**
     * Reads a {@code long} from the network, blocking until it is received.
     *
     * @return A big endian {@code long}.
     */
    public final long readLong() {
        return readLong(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a {@code long} from the network, blocking until it is received.
     *
     * @param byteOrder The order of the bytes in the {@code long}.
     * @return A {@code long} with the specified {@link ByteOrder byte order}.
     */
    public final long readLong(ByteOrder byteOrder) {
        var buffer = readHelper(Long.BYTES, byteOrder);
        long value = buffer.getLong();
        DIRECT_BUFFER_POOL.give(buffer);
        return value;
    }

    /**
     * Reads a {@code short} from the network, blocking until it is received.
     *
     * @return A big endian {@code short}.
     */
    public final short readShort() {
        return readShort(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a {@code short} from the network, blocking until it is received.
     *
     * @param byteOrder The order of the bytes in the {@code short}.
     * @return A {@code short} with the specified {@link ByteOrder byte order}.
     */
    public final short readShort(ByteOrder byteOrder) {
        var buffer = readHelper(Short.BYTES, byteOrder);
        short value = buffer.getShort();
        DIRECT_BUFFER_POOL.give(buffer);
        return value;
    }

    /**
     * Reads a {@link String} from the network, blocking until it is received.
     *
     * @return A big endian {@link String} with a {@link Charset charset} of {@link StandardCharsets#UTF_8}.
     */
    public final String readString() {
        return readString(StandardCharsets.UTF_8, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a {@link String} from the network, blocking until it is received.
     *
     * @param charset The {@link Charset} of the {@link String}.
     * @return A {@link String} with the specified {@link Charset charset}.
     */
    public final String readString(Charset charset) {
        return readString(charset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a {@link String} from the network, blocking until it is received.
     *
     * @param charset   The {@link Charset} of the {@link String}.
     * @param byteOrder The order of the bytes in the {@link String}.
     * @return A {@link String} with the specified {@link Charset charset} and {@link ByteOrder byte order}.
     */
    public final String readString(Charset charset, ByteOrder byteOrder) {
        short length = readShort(byteOrder);
        int newLength = length & 0xFFFF;
        var buffer = readHelper(Byte.BYTES * newLength, ByteOrder.BIG_ENDIAN);
        var b = new byte[newLength];
        buffer.get(b);
        DIRECT_BUFFER_POOL.give(buffer);
        return new String(b, charset);
    }

    /**
     * Registers a listener that fires right after a {@link Client client} disconnects from a {@link Server server}.
     * <br><br>
     * Calling this method more than once registers multiple listeners.
     *
     * @param listener A {@link Runnable} that will be executed when this {@link Client client} disconnects from the
     *                 {@link Server server}.
     */
    public final void onDisconnect(Runnable listener) {
        disconnectListeners.add(listener);
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
