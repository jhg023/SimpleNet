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
package com.github.simplenet.utility.exposed.data;

import com.github.simplenet.utility.exposed.consumer.ByteConsumer;
import com.github.simplenet.utility.exposed.predicate.BytePredicate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * An interface that defines the methods required to read {@code byte}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface ByteReader extends DataReader {
    
    /**
     * Requests a single {@code byte} and accepts a {@link ByteConsumer} with the {@code byte} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code byte} is received.
     */
    default void readByte(ByteConsumer consumer) {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.get()), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readByte(ByteConsumer)}; however, once finished, {@link #readByte(ByteConsumer)} is
     * called once again with the same consumer; this method loops until the specified {@link BytePredicate}
     * returns {@code false}, whereas {@link #readByte(ByteConsumer)} completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code byte} is received.
     */
    default void readByteUntil(BytePredicate predicate) {
        readUntil(Byte.BYTES, buffer -> predicate.test(buffer.get()), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readByte(ByteConsumer)}; however, once finished, {@link #readByte(ByteConsumer)} is called once
     * again with the same consumer; this method loops indefinitely, whereas {@link #readByte(ByteConsumer)}
     * completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code byte} is received.
     */
    default void readByteAlways(ByteConsumer consumer) {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.get()), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code byte[]} of length {@code n} and accepts a {@link Consumer} when all of the {@code byte}s are
     * received.
     *
     * @param n        The amount of {@code byte}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code byte}s are received.
     */
    default void readBytes(int n, Consumer<byte[]> consumer) {
        read(Byte.BYTES * n, buffer -> processBytes(buffer, n, consumer), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readBytes(int, Consumer)}; however, once finished, {@link #readBytes(int, Consumer)} is called
     * once again with the same parameter; this loops indefinitely, whereas {@link #readBytes(int, Consumer)}
     * completes after a single iteration.
     *
     * @param n        The amount of {@code byte}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code byte}s are received.
     */
    default void readBytesAlways(int n, Consumer<byte[]> consumer) {
        readAlways(Byte.BYTES * n, buffer -> processBytes(buffer, n, consumer), ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link ByteBuffer} that contains the bytes needed.
     * @param n          The amount of bytes requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} bytes are received.
     */
    private void processBytes(ByteBuffer buffer, int n, Consumer<byte[]> consumer) {
        var b = new byte[n];
        buffer.get(b);
        consumer.accept(b);
    }
}
