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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import simplenet.packet.Packet;

final class PacketTest {
    
    private Packet packet;
    
    @BeforeEach
    void initialize() {
        packet = Packet.builder();
    }
    
    @Test
    void testDifferentPacketsAreActuallyDifferent() {
        Assertions.assertNotEquals(Packet.builder(), Packet.builder());
    }
    
    @ParameterizedTest
    @ValueSource(bytes = { Byte.MIN_VALUE, 0, Byte.MAX_VALUE })
    void testPutByteIntoPacket(byte b) {
        packet.putByte(b);
        
        Assertions.assertEquals(packet.getSize(), Byte.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { b });
    }
    
    @Test
    void testPutBytesIntoPacket() {
        packet.putBytes((byte) 42, (byte) 123, (byte) -25, (byte) 75);
    
        Assertions.assertEquals(packet.getSize(), Byte.BYTES * 4);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { 42, 123, -25, 75 });
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    void testPutBooleanIntoPacket(String s) {
        boolean b = Boolean.parseBoolean(s);
        
        packet.putBoolean(b);
        
        Assertions.assertEquals(packet.getSize(), Byte.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { (byte) (b ? 1 : 0) });
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\0', Character.MAX_VALUE })
    void testPutCharBigEndianIntoPacket(char c) {
        packet.putChar(c);
        
        Assertions.assertEquals(packet.getSize(), Character.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        byte[] bytes = new byte[] { (byte) (c >> 8), (byte) c };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(chars = { Character.MIN_VALUE, '\0', Character.MAX_VALUE })
    void testPutCharLittleEndianIntoPacket(char c) {
        packet.putChar(c, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Character.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        byte[] bytes = new byte[] { (byte) c, (byte) (c >> 8) };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, 0, Short.MAX_VALUE })
    void testPutShortBigEndianIntoPacket(short s) {
        packet.putShort(s);
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        byte[] bytes = new byte[] { (byte) (s >> 8), (byte) s };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(shorts = { Short.MIN_VALUE, 0, Short.MAX_VALUE })
    void testPutShortLittleEndianIntoPacket(short s) {
        packet.putShort(s, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        byte[] bytes = new byte[] { (byte) s, (byte) (s >> 8) };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, 0, Integer.MAX_VALUE })
    void testPutIntBigEndianIntoPacket(int i) {
        packet.putInt(i);
        
        Assertions.assertEquals(packet.getSize(), Integer.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        byte[] bytes = new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(ints = { Integer.MIN_VALUE, 0, Integer.MAX_VALUE })
    void testPutIntLittleEndianIntoPacket(int i) {
        packet.putInt(i, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Integer.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        byte[] bytes = new byte[] { (byte) i, (byte) (i >> 8), (byte) (i >> 16), (byte) (i >> 24) };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, 0f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY })
    void testPutFloatBigEndianIntoPacket(float f) {
        packet.putFloat(f);
        
        Assertions.assertEquals(packet.getSize(), Float.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        int i = Float.floatToRawIntBits(f);
        
        byte[] bytes = new byte[] { (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(floats = { Float.MIN_VALUE, 0f, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY })
    void testPutFloatLittleEndianIntoPacket(float f) {
        packet.putFloat(f, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Float.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        int i = Float.floatToRawIntBits(f);
    
        byte[] bytes = new byte[] { (byte) i, (byte) (i >> 8), (byte) (i >> 16), (byte) (i >> 24) };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, 0L, Long.MAX_VALUE })
    void testPutLongBigEndianIntoPacket(long l) {
        packet.putLong(l);
        
        Assertions.assertEquals(packet.getSize(), Long.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        byte[] bytes = new byte[] {
            (byte) (l >> 56), (byte) (l >> 48), (byte) (l >> 40), (byte) (l >> 32),
            (byte) (l >> 24), (byte) (l >> 16), (byte) (l >> 8), (byte) l
        };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(longs = { Long.MIN_VALUE, 0L, Long.MAX_VALUE })
    void testPutLongLittleEndianIntoPacket(long l) {
        packet.putLong(l, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Long.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        byte[] bytes = new byte[] {
            (byte) l, (byte) (l >> 8), (byte) (l >> 16), (byte) (l >> 24),
            (byte) (l >> 32), (byte) (l >> 40), (byte) (l >> 48), (byte) (l >> 56)
        };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, 0D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
    void testPutDoubleBigEndianIntoPacket(double d) {
        packet.putDouble(d);
        
        Assertions.assertEquals(packet.getSize(), Double.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        long l = Double.doubleToRawLongBits(d);
        
        byte[] bytes = new byte[] {
            (byte) (l >> 56), (byte) (l >> 48), (byte) (l >> 40), (byte) (l >> 32),
            (byte) (l >> 24), (byte) (l >> 16), (byte) (l >> 8), (byte) l
        };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(doubles = { Double.MIN_VALUE, 0D, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
    void testPutDoubleLittleEndianIntoPacket(double d) {
        packet.putDouble(d, ByteOrder.LITTLE_ENDIAN);
        
        Assertions.assertEquals(packet.getSize(), Double.BYTES);
        Assertions.assertEquals(packet.getQueue().size(), 1);
        
        long l = Double.doubleToRawLongBits(d);
    
        byte[] bytes = new byte[] {
            (byte) l, (byte) (l >> 8), (byte) (l >> 16), (byte) (l >> 24),
            (byte) (l >> 32), (byte) (l >> 40), (byte) (l >> 48), (byte) (l >> 56)
        };
        
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testPutStringBigEndianUTF8IntoPacket(String s) {
        packet.putString(s);
        
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        short length = (short) bytes.length;
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES + length);
        Assertions.assertEquals(packet.getQueue().size(), 2);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { (byte) (length >> 8), (byte) length });
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testPutStringBigEndianUTF16IntoPacket(String s) {
        packet.putString(s, StandardCharsets.UTF_16);
    
        byte[] bytes = s.getBytes(StandardCharsets.UTF_16);
        short length = (short) bytes.length;
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES + length);
        Assertions.assertEquals(packet.getQueue().size(), 2);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { (byte) (length >> 8), (byte) length });
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testPutStringLittleEndianUTF8IntoPacket(String s) {
        packet.putString(s, StandardCharsets.UTF_8, ByteOrder.LITTLE_ENDIAN);
    
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        short length = (short) bytes.length;
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES + length);
        Assertions.assertEquals(packet.getQueue().size(), 2);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { (byte) length, (byte) (length >> 8) });
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @ParameterizedTest
    @ValueSource(strings = { "Hello World!" })
    void testPutStringLittleEndianUTF16IntoPacket(String s) {
        packet.putString(s, StandardCharsets.UTF_16, ByteOrder.LITTLE_ENDIAN);
        
        byte[] bytes = s.getBytes(StandardCharsets.UTF_16);
        short length = (short) bytes.length;
        
        Assertions.assertEquals(packet.getSize(), Short.BYTES + length);
        Assertions.assertEquals(packet.getQueue().size(), 2);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { (byte) length, (byte) (length >> 8) });
        Assertions.assertArrayEquals(packet.getQueue().poll(), bytes);
    }
    
    @Test
    void testPutMultipleValuesIntoPacket() {
        packet.putByte(42).putByte(-24).putByte(123);
    
        Assertions.assertEquals(packet.getSize(), Byte.BYTES * 3);
        Assertions.assertEquals(packet.getQueue().size(), 3);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { 42 });
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { -24 });
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { 123 });
    }
    
    @Test
    void testPrependMultipleValuesOntoPacket() {
        packet.putByte(42).putByte(-24).prepend(p -> p.putByte(75).putByte(64)).putByte(123);
        
        Assertions.assertEquals(packet.getSize(), Byte.BYTES * 5);
        Assertions.assertEquals(packet.getQueue().size(), 5);
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { 75 });
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { 64 });
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { 42 });
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { -24 });
        Assertions.assertArrayEquals(packet.getQueue().poll(), new byte[] { 123 });
    }

}
