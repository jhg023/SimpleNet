package com.github.simplenet;/*
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

import com.github.simplenet.packet.Packet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

final class ReadTest {

    private static final String HOST = "localhost";
    
    private static final int PORT = 43594;
    
    private Client client;
    
    private Server server;
    
    private CountDownLatch latch;
    
    @BeforeEach
    void beforeEach() {
        client = new Client();
        server = new Server();
        latch = new CountDownLatch(1);
        server.bind(HOST, PORT, 1);
    }
    
    @AfterEach
    void afterEach() throws InterruptedException {
        client.connect(HOST, PORT);
        
        try {
            if (!latch.await(500L, TimeUnit.MILLISECONDS)) {
                fail();
            }
        } finally {
            client.close();
            server.close();
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "true", "false"})
    void testReadBoolean(String s) {
        boolean b = Boolean.parseBoolean(s);
        client.onConnect(() -> Packet.builder().putBoolean(b).queueAndFlush(client));
        server.onConnect(client -> client.readBoolean(readBoolean -> {
            assertEquals(b, readBoolean);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(bytes = { Byte.MIN_VALUE, -32, 0, 32, Byte.MAX_VALUE })
    void testReadByte(byte b) {
        client.onConnect(() -> Packet.builder().putByte(b).queueAndFlush(client));
        server.onConnect(client -> client.readByte(readByte -> {
            assertEquals(b, readByte);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\u1234', '\u8000', Character.MAX_VALUE })
    void testReadCharBigEndian(char c) {
        client.onConnect(() -> Packet.builder().putChar(c).queueAndFlush(client));
        server.onConnect(client -> client.readChar(readChar -> {
            assertEquals(c, readChar);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\u1234', '\u8000', Character.MAX_VALUE })
    void testReadCharLittleEndian(char c) {
        client.onConnect(() -> Packet.builder().putChar(c, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client));
        server.onConnect(client -> client.readChar(readChar -> {
            assertEquals(c, readChar);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, -32, 0, 32, Short.MAX_VALUE })
    void testReadShortBigEndian(short s) {
        client.onConnect(() -> Packet.builder().putShort(s).queueAndFlush(client));
        server.onConnect(client -> client.readShort(readShort -> {
            assertEquals(s, readShort);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, -32, 0, 32, Short.MAX_VALUE })
    void testReadShortLittleEndian(short s) {
        client.onConnect(() -> Packet.builder().putShort(s, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client));
        server.onConnect(client -> client.readShort(readShort -> {
            assertEquals(s, readShort);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -32, 0, 32, Integer.MAX_VALUE })
    void testReadIntBigEndian(int i) {
        client.onConnect(() -> Packet.builder().putInt(i).queueAndFlush(client));
        server.onConnect(client -> client.readInt(readInt -> {
            assertEquals(i, readInt);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -32, 0, 32, Integer.MAX_VALUE })
    void testReadIntLittleEndian(int i) {
        client.onConnect(() -> Packet.builder().putInt(i, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client));
        server.onConnect(client -> client.readInt(readInt -> {
            assertEquals(i, readInt);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, -32.5f, 0f, 32.5f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY })
    void testReadFloatBigEndian(float f) {
        client.onConnect(() -> Packet.builder().putFloat(f).queueAndFlush(client));
        server.onConnect(client -> client.readFloat(readFloat -> {
            assertEquals(f, readFloat);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, -32.5f, 0f, 32.5f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY })
    void testReadFloatLittleEndian(float f) {
        client.onConnect(() -> Packet.builder().putFloat(f, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client));
        server.onConnect(client -> client.readFloat(readFloat -> {
            assertEquals(f, readFloat);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -32L, 0L, 32L, Long.MAX_VALUE })
    void testReadLongBigEndian(long l) {
        client.onConnect(() -> Packet.builder().putLong(l).queueAndFlush(client));
        server.onConnect(client -> client.readLong(readLong -> {
            assertEquals(l, readLong);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -32L, 0L, 32L, Long.MAX_VALUE })
    void testReadLongLittleEndian(long l) {
        client.onConnect(() -> Packet.builder().putLong(l, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client));
        server.onConnect(client -> client.readLong(readLong -> {
            assertEquals(l, readLong);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, -32.5D, 0D, 32.5D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY })
    void testReadDoubleBigEndian(double d) {
        client.onConnect(() -> Packet.builder().putDouble(d).queueAndFlush(client));
        server.onConnect(client -> client.readDouble(readDouble -> {
            assertEquals(d, readDouble);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, -32.5D, 0D, 32.5D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY })
    void testReadDoubleLittleEndian(double d) {
        client.onConnect(() -> Packet.builder().putDouble(d, ByteOrder.LITTLE_ENDIAN).queueAndFlush(client));
        server.onConnect(client -> client.readDouble(readDouble -> {
            assertEquals(d, readDouble);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
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
        client.onConnect(() -> Packet.builder().putBytes(first, second, third).queueAndFlush(client));
        server.onConnect(client -> {
            client.readByte(readByte -> {
                assertEquals(first, readByte);
                latch.countDown();
            });
            
            client.readByte(readByte -> {
                assertEquals(second, readByte);
                latch.countDown();
            });
            
            client.readByte(readByte -> {
                assertEquals(third, readByte);
                latch.countDown();
            });
        });
    }
    
    @Test
    void testReadNestedCallbacks() {
        final byte first = 42, second = -24, third = 123;
        client.onConnect(() -> Packet.builder().putBytes(first, second, third).queueAndFlush(client));
        server.onConnect(client -> {
            client.readByte(readFirstByte -> {
                assertEquals(first, readFirstByte);
                
                client.readByte(readSecondByte -> {
                    assertEquals(second, readSecondByte);
    
                    client.readByte(readThirdByte -> {
                        assertEquals(third, readThirdByte);
                        latch.countDown();
                    });
                });
            });
        });
    }
    
    @Test
    void testReadNestedCallbacksExecuteInCorrectOrder() {
        final byte[] bytes = {42, -24, 123, 32, 3};
        Deque<Byte> queue = new ArrayDeque<>();
        client.onConnect(() -> Packet.builder().putBytes(bytes).queueAndFlush(client));
        server.onConnect(client -> {
            client.readByte(first -> {
                queue.offer(first);
                
                client.readByte(second -> {
                    queue.offer(second);
                    client.readByte(queue::offer);
                });
            });
    
            client.readByte(fourth -> {
                queue.offer(fourth);
                
                client.readByte(fifth -> {
                    queue.offer(fifth);
    
                    assertEquals(bytes.length, queue.size());
                    
                    for (byte b : bytes) {
                        assertEquals(b, queue.poll());
                    }
                    
                    latch.countDown();
                });
            });
        });
    }
    
    @Test
    void readWithSmallBuffer() {
        byte b = 42;
        long l = ThreadLocalRandom.current().nextLong();
        client = new Client();
        client.onConnect(() -> {
            Packet.builder().putByte(b).queue(client);
            Packet.builder().putLong(l).queueAndFlush(client);
        });
        server.close();
        server = new Server();
        server.bind(HOST, PORT);
        server.onConnect(client -> {
            client.readByte(first -> {
                assertEquals(b, first);
                
                client.readLong(second -> {
                    assertEquals(l, second);
                    latch.countDown();
                });
            });
        });
    }

    @Test
    void testDoNotReadEveryByteFromBuffer() {
        long first = ThreadLocalRandom.current().nextLong(), second = ThreadLocalRandom.current().nextLong(),
            third = ThreadLocalRandom.current().nextLong();
        client.onConnect(() -> {
            Packet.builder().putLong(first).putLong(second).putLong(third).queueAndFlush(client);
        });
        server.onConnect(client -> {
            client.read(Long.BYTES * 2, buffer -> {});
            client.readLong(readThird -> {
                assertEquals(third, readThird);
                latch.countDown();
            });
        });
    }

    @Test
    void testCanWriteSamePacketObjectMoreThanOnce() {
        client.onConnect(() -> {
            Packet packet = Packet.builder().putByte(42);

            packet.queueAndFlush(client);
            packet.queueAndFlush(client);
        });
        server.onConnect(client -> {
            client.readByte(firstByte -> {
                assertEquals(42, firstByte);

                client.readByte(secondByte -> {
                    assertEquals(42, secondByte);
                    latch.countDown();
                });
            });
        });
    }
    
    private void readStringHelper(String s, Charset charset, ByteOrder order) {
        client.onConnect(() -> Packet.builder().putString(s, charset, order).queueAndFlush(client));
        server.onConnect(client -> client.readString(readString -> {
            assertEquals(s, readString);
            latch.countDown();
        }, charset, order));
    }
}
