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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

final class PacketTest {
    
    private Packet packet;
    
    @BeforeEach
    void beforeEach() {
        packet = Packet.builder();
    }
    
    @Test
    void testDifferentPacketsAreActuallyDifferent() {
        Assertions.assertNotEquals(Packet.builder(), Packet.builder());
    }
    
    @ParameterizedTest
    @ValueSource(bytes = { Byte.MIN_VALUE, -32, 0, 32, Byte.MAX_VALUE })
    void testPutByteIntoPacket(byte b) {
        packet.putByte(b);
        
        Assertions.assertEquals(packet.getSize(), Byte.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(b, buffer.get());
    }
    
    @Test
    void testPutBytesIntoPacket() {
        packet.putBytes((byte) 42, (byte) 123, (byte) -25, (byte) 75);
    
        Assertions.assertEquals(packet.getSize(), Byte.BYTES * 4);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES * 4);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(42, buffer.get());
        Assertions.assertEquals(123, buffer.get());
        Assertions.assertEquals(-25, buffer.get());
        Assertions.assertEquals(75, buffer.get());
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    void testPutBooleanIntoPacket(String s) {
        boolean b = Boolean.parseBoolean(s);
        
        packet.putBoolean(b);
        
        Assertions.assertEquals(packet.getSize(), Byte.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(b ? 1 : 0, buffer.get());
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\u1234', '\u8000', Character.MAX_VALUE })
    void testPutCharBigEndianIntoPacket(char c) {
        packet.putChar(c);
        
        Assertions.assertEquals(packet.getSize(), Character.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Character.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(c, buffer.getChar());
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\u1234', '\u8000', Character.MAX_VALUE })
    void testPutCharLittleEndianIntoPacket(char c) {
        packet.putChar(c, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Character.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Character.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(c, buffer.getChar());
    }
    
    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, -32, 0, 32, Short.MAX_VALUE })
    void testPutShortBigEndianIntoPacket(short s) {
        packet.putShort(s);
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(s, buffer.getShort());
    }
    
    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, -32, 0, 32, Short.MAX_VALUE })
    void testPutShortLittleEndianIntoPacket(short s) {
        packet.putShort(s, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(s, buffer.getShort());
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -32, 0, 32, Integer.MAX_VALUE })
    void testPutIntBigEndianIntoPacket(int i) {
        packet.putInt(i);
        
        Assertions.assertEquals(packet.getSize(), Integer.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(i, buffer.getInt());
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, -32, 0, 32, Integer.MAX_VALUE })
    void testPutIntLittleEndianIntoPacket(int i) {
        packet.putInt(i, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Integer.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(i, buffer.getInt());
    }
    
    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, -32.5f, 0f, 32.5f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY })
    void testPutFloatBigEndianIntoPacket(float f) {
        packet.putFloat(f);
        
        Assertions.assertEquals(packet.getSize(), Float.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(f, buffer.getFloat());
    }
    
    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, -32.5f, 0f, 32.5f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY })
    void testPutFloatLittleEndianIntoPacket(float f) {
        packet.putFloat(f, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Float.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(f, buffer.getFloat());
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -32L, 0L, 32L, Long.MAX_VALUE })
    void testPutLongBigEndianIntoPacket(long l) {
        packet.putLong(l);
        
        Assertions.assertEquals(packet.getSize(), Long.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(l, buffer.getLong());
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, -32L, 0L, 32L, Long.MAX_VALUE })
    void testPutLongLittleEndianIntoPacket(long l) {
        packet.putLong(l, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Long.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(l, buffer.getLong());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, -32.5D, 0D, 32.5D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
    void testPutDoubleBigEndianIntoPacket(double d) {
        packet.putDouble(d);
        
        Assertions.assertEquals(packet.getSize(), Double.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(d, buffer.getDouble());
    }
    
    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, -32.5D, 0D, 32.5D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
    void testPutDoubleLittleEndianIntoPacket(double d) {
        packet.putDouble(d, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Double.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);

        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(d, buffer.getDouble());
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testPutStringBigEndianUTF8IntoPacket(String s) {
        packet.putString(s);
        
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        short length = (short) bytes.length;
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES + length);
        Assertions.assertEquals(packet.getQueue().size(), 2);

        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES + length);
        packet.getQueue().poll().accept(buffer);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(length, buffer.getShort());

        byte[] data = new byte[length];
        buffer.get(data);
        Assertions.assertArrayEquals(bytes, data);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testPutStringBigEndianUTF16IntoPacket(String s) {
        packet.putString(s, StandardCharsets.UTF_16);
    
        byte[] bytes = s.getBytes(StandardCharsets.UTF_16);
        short length = (short) bytes.length;
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES + length);
        Assertions.assertEquals(packet.getQueue().size(), 2);

        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES + length);
        packet.getQueue().poll().accept(buffer);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        Assertions.assertEquals(length, buffer.getShort());

        byte[] data = new byte[length];
        buffer.get(data);
        Assertions.assertArrayEquals(bytes, data);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testPutStringLittleEndianUTF8IntoPacket(String s) {
        packet.putString(s, StandardCharsets.UTF_8, ByteOrder.LITTLE_ENDIAN);
    
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        short length = (short) bytes.length;
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES + length);
        Assertions.assertEquals(packet.getQueue().size(), 2);

        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES + length);
        packet.getQueue().poll().accept(buffer);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(length, buffer.getShort());

        byte[] data = new byte[length];
        buffer.get(data);
        Assertions.assertArrayEquals(bytes, data);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testPutStringLittleEndianUTF16IntoPacket(String s) {
        packet.putString(s, StandardCharsets.UTF_16, ByteOrder.LITTLE_ENDIAN);
        
        byte[] bytes = s.getBytes(StandardCharsets.UTF_16);
        short length = (short) bytes.length;
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES + length);
        Assertions.assertEquals(packet.getQueue().size(), 2);

        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES + length);
        packet.getQueue().poll().accept(buffer);
        packet.getQueue().poll().accept(buffer);
        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Assertions.assertEquals(length, buffer.getShort());

        byte[] data = new byte[length];
        buffer.get(data);
        Assertions.assertArrayEquals(bytes, data);
    }
    
    @Test
    void testPutMultipleBytesIntoPacket() {
        packet.putByte(42).putByte(-24).putByte(123);
    
        Assertions.assertEquals(packet.getSize(), Byte.BYTES * 3);
        Assertions.assertEquals(packet.getQueue().size(), 3);

        ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES * 3);
        for (int i = 0; i < 3; i++) {
            packet.getQueue().poll().accept(buffer);
        }
        buffer.flip();
        Assertions.assertEquals(42, buffer.get());
        Assertions.assertEquals(-24, buffer.get());
        Assertions.assertEquals(123, buffer.get());
    }
    
    @Test
    void testPrependMultipleBytesOntoPacket() {
        packet.putByte(42).putByte(-24).prepend(p -> p.putByte(75).putByte(64)).putByte(123);
        
        Assertions.assertEquals(packet.getSize(), Byte.BYTES * 5);
        Assertions.assertEquals(packet.getQueue().size(), 5);

        ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES * 5);
        for (int i = 0; i < 5; i++) {
            packet.getQueue().poll().accept(buffer);
        }
        buffer.flip();
        Assertions.assertEquals(75, buffer.get());
        Assertions.assertEquals(64, buffer.get());
        Assertions.assertEquals(42, buffer.get());
        Assertions.assertEquals(-24, buffer.get());
        Assertions.assertEquals(123, buffer.get());
    }
}
