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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

final class ReadTest {

    private static final String HOST = "localhost";

    private static final AtomicInteger PORT = new AtomicInteger(43_594);

    private Client client;

    private Server server;

    private CountDownLatch latch;

    private Consumer<Client> clientConsumer;

    @BeforeEach
    void beforeEach() {
        client = new Client();
        server = new Server();
        latch = new CountDownLatch(1);
    }
    
    @AfterEach
    void afterEach() throws InterruptedException {
        int port = PORT.getAndUpdate(i -> {
            server.bind(HOST, i);
            return i + 1;
        });
        client.connect(HOST, port);

        clientConsumer.accept(client);

        try {
            if (!latch.await(1L, TimeUnit.SECONDS)) {
                fail();
            }
        } finally {
            client.close();
            server.close();
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    void testReadBoolean(String s) {
        boolean b = Boolean.parseBoolean(s);
        clientConsumer = client -> Packet.builder().putBoolean(b).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(b, client.readBoolean());
            latch.countDown();
        });
    }
    
    @ParameterizedTest
    @ValueSource(bytes = { Byte.MIN_VALUE, -32, 0, 32, Byte.MAX_VALUE })
    void testReadByte(byte b) {
        clientConsumer = client -> Packet.builder().putByte(b).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(b, client.readByte());
            latch.countDown();
        });
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\u1234', '\u8000', Character.MAX_VALUE })
    void testReadCharBigEndian(char c) {
        clientConsumer = client -> Packet.builder().putChar(c).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(c, client.readChar());
            latch.countDown();
        });
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\u1234', '\u8000', Character.MAX_VALUE })
    void testReadCharLittleEndian(char c) {
        clientConsumer = client -> Packet.builder().putChar(c, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(c, client.readChar(ByteOrder.LITTLE_ENDIAN));
            latch.countDown();
        });
    }

    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, -32.5D, 0D, 32.5D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY })
    void testReadDoubleBigEndian(double d) {
        clientConsumer = client -> Packet.builder().putDouble(d).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(d, client.readDouble());
            latch.countDown();
        });
    }

    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, -32.5D, 0D, 32.5D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY })
    void testReadDoubleLittleEndian(double d) {
        clientConsumer = client -> Packet.builder().putDouble(d, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(d, client.readDouble(ByteOrder.LITTLE_ENDIAN));
            latch.countDown();
        });
    }

    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, -32.5f, 0f, 32.5f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY })
    void testReadFloatBigEndian(float f) {
        clientConsumer = client -> Packet.builder().putFloat(f).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(f, client.readFloat());
            latch.countDown();
        });
    }

    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, -32.5f, 0f, 32.5f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY })
    void testReadFloatLittleEndian(float f) {
        clientConsumer = client -> Packet.builder().putFloat(f, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(f, client.readFloat(ByteOrder.LITTLE_ENDIAN));
            latch.countDown();
        });
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -32, 0, 32, Integer.MAX_VALUE })
    void testReadIntBigEndian(int i) {
        clientConsumer = client -> Packet.builder().putInt(i).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(i, client.readInt());
            latch.countDown();
        });
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -32, 0, 32, Integer.MAX_VALUE })
    void testReadIntLittleEndian(int i) {
        clientConsumer = client -> Packet.builder().putInt(i, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(i, client.readInt(ByteOrder.LITTLE_ENDIAN));
            latch.countDown();
        });
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -32L, 0L, 32L, Long.MAX_VALUE })
    void testReadLongBigEndian(long l) {
        clientConsumer = client -> Packet.builder().putLong(l).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(l, client.readLong());
            latch.countDown();
        });
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -32L, 0L, 32L, Long.MAX_VALUE })
    void testReadLongLittleEndian(long l) {
        clientConsumer = client -> Packet.builder().putLong(l, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(l, client.readLong(ByteOrder.LITTLE_ENDIAN));
            latch.countDown();
        });
    }

    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, -32, 0, 32, Short.MAX_VALUE })
    void testReadShortBigEndian(short s) {
        clientConsumer = client -> Packet.builder().putShort(s).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(s, client.readShort());
            latch.countDown();
        });
    }

    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, -32, 0, 32, Short.MAX_VALUE })
    void testReadShortLittleEndian(short s) {
        clientConsumer = client -> Packet.builder().putShort(s, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(s, client.readShort(ByteOrder.LITTLE_ENDIAN));
            latch.countDown();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringBigEndianUTF_8(String s) {
        readStringHelper(s, StandardCharsets.UTF_8, ByteOrder.BIG_ENDIAN);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringBigEndianUTF_16(String s) {
        readStringHelper(s, StandardCharsets.UTF_16, ByteOrder.BIG_ENDIAN);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringBigEndianUTF_16BE(String s) {
        readStringHelper(s, StandardCharsets.UTF_16BE, ByteOrder.BIG_ENDIAN);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringBigEndianUTF_16LE(String s) {
        readStringHelper(s, StandardCharsets.UTF_16LE, ByteOrder.BIG_ENDIAN);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringLittleEndianUTF_8(String s) {
        readStringHelper(s, StandardCharsets.UTF_8, ByteOrder.LITTLE_ENDIAN);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringLittleEndianUTF_16(String s) {
        readStringHelper(s, StandardCharsets.UTF_16, ByteOrder.LITTLE_ENDIAN);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringLittleEndianUTF_16BE(String s) {
        readStringHelper(s, StandardCharsets.UTF_16BE, ByteOrder.LITTLE_ENDIAN);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringLittleEndianUTF_16LE(String s) {
        readStringHelper(s, StandardCharsets.UTF_16LE, ByteOrder.LITTLE_ENDIAN);
    }
    
    @Test
    void testReadMultipleValues() {
        final byte first = 42, second = -24, third = 123;
        latch = new CountDownLatch(3);
        clientConsumer = client -> Packet.builder().putBytes(first, second, third).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(first, client.readByte());
            latch.countDown();

            assertEquals(second, client.readByte());
            latch.countDown();

            assertEquals(third, client.readByte());
            latch.countDown();
        });
    }

    @Test
    void testDoNotReadEveryByteFromBuffer() {
        long first = ThreadLocalRandom.current().nextLong(), second = ThreadLocalRandom.current().nextLong(),
            third = ThreadLocalRandom.current().nextLong();
        clientConsumer = client -> Packet.builder().putLong(first).putLong(second).putLong(third).queueAndFlush(client);
        server.onConnect(client -> {
            client.read(Long.BYTES * 2, buffer -> {});
            assertEquals(third, client.readLong());
            latch.countDown();
        });
    }

    @Test
    void testCanWriteSamePacketObjectMoreThanOnce() {
        clientConsumer = client -> {
            Packet packet = Packet.builder().putByte(42);

            packet.queueAndFlush(client);
            packet.queueAndFlush(client);
        };
        server.onConnect(client -> {
            assertEquals(42, client.readByte());
            assertEquals(42, client.readByte());
            latch.countDown();
        });
    }
    
    private void readStringHelper(String s, Charset charset, ByteOrder order) {
        clientConsumer = client -> Packet.builder().putString(s, charset, order).queueAndFlush(client);
        server.onConnect(client -> {
            assertEquals(s, client.readString(charset, order));
            latch.countDown();
        });
    }
}
