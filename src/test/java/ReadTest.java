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

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import simplenet.Client;
import simplenet.Server;
import simplenet.packet.Packet;


import static org.junit.jupiter.api.Assertions.assertEquals;

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
        server.bind(HOST, PORT);
    }
    
    @AfterEach
    void afterEach() throws InterruptedException {
        client.connect(HOST, PORT);
        
        try {
            if (!latch.await(500L, TimeUnit.MILLISECONDS)) {
                Assertions.fail();
            }
        } finally {
            client.close();
            server.close();
        }
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "true", "false"})
    void testReadBoolean(String s) {
        var b = Boolean.parseBoolean(s);
        client.onConnect(() -> Packet.builder().putBoolean(b).writeAndFlush(client));
        server.onConnect(client -> client.readBoolean(readBoolean -> {
            assertEquals(b, readBoolean);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(bytes = { Byte.MIN_VALUE, -32, 0, 32, Byte.MAX_VALUE })
    void testReadByte(byte b) {
        client.onConnect(() -> Packet.builder().putByte(b).writeAndFlush(client));
        server.onConnect(client -> client.readByte(readByte -> {
            assertEquals(b, readByte);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\u1234', '\u8000', Character.MAX_VALUE })
    void testReadCharBigEndian(char c) {
        client.onConnect(() -> Packet.builder().putChar(c).writeAndFlush(client));
        server.onConnect(client -> client.readChar(readChar -> {
            assertEquals(c, readChar);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\u1234', '\u8000', Character.MAX_VALUE })
    void testReadCharLittleEndian(char c) {
        client.onConnect(() -> Packet.builder().putChar(c, ByteOrder.LITTLE_ENDIAN).writeAndFlush(client));
        server.onConnect(client -> client.readChar(readChar -> {
            assertEquals(c, readChar);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, -32, 0, 32, Short.MAX_VALUE })
    void testReadShortBigEndian(short s) {
        client.onConnect(() -> Packet.builder().putShort(s).writeAndFlush(client));
        server.onConnect(client -> client.readShort(readShort -> {
            assertEquals(s, readShort);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, -32, 0, 32, Short.MAX_VALUE })
    void testReadShortLittleEndian(short s) {
        client.onConnect(() -> Packet.builder().putShort(s, ByteOrder.LITTLE_ENDIAN).writeAndFlush(client));
        server.onConnect(client -> client.readShort(readShort -> {
            assertEquals(s, readShort);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -32, 0, 32, Integer.MAX_VALUE })
    void testReadIntBigEndian(int i) {
        client.onConnect(() -> Packet.builder().putInt(i).writeAndFlush(client));
        server.onConnect(client -> client.readInt(readInt -> {
            assertEquals(i, readInt);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -32, 0, 32, Integer.MAX_VALUE })
    void testReadIntLittleEndian(int i) {
        client.onConnect(() -> Packet.builder().putInt(i, ByteOrder.LITTLE_ENDIAN).writeAndFlush(client));
        server.onConnect(client -> client.readInt(readInt -> {
            assertEquals(i, readInt);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, -32.5f, 0f, 32.5f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY })
    void testReadFloatBigEndian(float f) {
        client.onConnect(() -> Packet.builder().putFloat(f).writeAndFlush(client));
        server.onConnect(client -> client.readFloat(readFloat -> {
            assertEquals(f, readFloat);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, -32.5f, 0f, 32.5f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY })
    void testReadFloatLittleEndian(float f) {
        client.onConnect(() -> Packet.builder().putFloat(f, ByteOrder.LITTLE_ENDIAN).writeAndFlush(client));
        server.onConnect(client -> client.readFloat(readFloat -> {
            assertEquals(f, readFloat);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -32L, 0L, 32L, Long.MAX_VALUE })
    void testReadLongBigEndian(long l) {
        client.onConnect(() -> Packet.builder().putLong(l).writeAndFlush(client));
        server.onConnect(client -> client.readLong(readLong -> {
            assertEquals(l, readLong);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -32L, 0L, 32L, Long.MAX_VALUE })
    void testReadLongLittleEndian(long l) {
        client.onConnect(() -> Packet.builder().putLong(l, ByteOrder.LITTLE_ENDIAN).writeAndFlush(client));
        server.onConnect(client -> client.readLong(readLong -> {
            assertEquals(l, readLong);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, -32.5D, 0D, 32.5D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
    void testReadDoubleBigEndian(double d) {
        client.onConnect(() -> Packet.builder().putDouble(d).writeAndFlush(client));
        server.onConnect(client -> client.readDouble(readDouble -> {
            assertEquals(d, readDouble);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, -32.5D, 0D, 32.5D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
    void testReadDoubleLittleEndian(double d) {
        client.onConnect(() -> Packet.builder().putDouble(d, ByteOrder.LITTLE_ENDIAN).writeAndFlush(client));
        server.onConnect(client -> client.readDouble(readDouble -> {
            assertEquals(d, readDouble);
            latch.countDown();
        }, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringBigEndianUTF8(String s) {
        client.onConnect(() -> Packet.builder().putString(s).writeAndFlush(client));
        server.onConnect(client -> client.readString(readString -> {
            assertEquals(s, readString);
            latch.countDown();
        }));
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringBigEndianUTF16(String s) {
        client.onConnect(() -> Packet.builder().putString(s, StandardCharsets.UTF_16).writeAndFlush(client));
        server.onConnect(client -> client.readString(readString -> {
            assertEquals(s, readString);
            latch.countDown();
        }, StandardCharsets.UTF_16));
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringLittleEndianUTF8(String s) {
        client.onConnect(() -> Packet.builder().putString(s, StandardCharsets.UTF_8, ByteOrder.LITTLE_ENDIAN).writeAndFlush(client));
        server.onConnect(client -> client.readString(readString -> {
            assertEquals(s, readString);
            latch.countDown();
        }, StandardCharsets.UTF_8, ByteOrder.LITTLE_ENDIAN));
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testReadStringLittleEndianUTF16(String s) {
        client.onConnect(() -> Packet.builder().putString(s, StandardCharsets.UTF_16, ByteOrder.LITTLE_ENDIAN).writeAndFlush(client));
        server.onConnect(client -> client.readString(readString -> {
            assertEquals(s, readString);
            latch.countDown();
        }, StandardCharsets.UTF_16, ByteOrder.LITTLE_ENDIAN));
    }
    
    @Test
    void testPutMultipleBytesIntoPacket() {
        int first = 42, second = -24, third = 123;
        latch = new CountDownLatch(3);
        client.onConnect(() -> Packet.builder().putByte(first).putByte(second).putByte(third).writeAndFlush(client));
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
    
}
