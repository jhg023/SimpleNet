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

import javax.crypto.Cipher;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An interface that defines the methods required to read data over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
@FunctionalInterface
public interface DataReader {
    
    /**
     * Requests {@code n} bytes and accepts a {@link Consumer} with them (in a {@link ByteBuffer}) (with
     * {@link ByteOrder#BIG_ENDIAN} order) once received.
     * <br><br>
     * If the amount of bytes requested already reside in the buffer, then this method may block to accept the
     * {@link Consumer} with the bytes. Otherwise, it simply queues up a request for the bytes, which does not block.
     * <br><br>
     * If encryption is active with a {@link Cipher} that uses padding, then this method should <strong>not</strong> be
     * called directly, as each grouping of bytes ({@code byte}, {@code short}, {@code int}, etc.) is encrypted
     * separately and will most-likely not reflect the amount of bytes requested.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     */
    default void read(int n, Consumer<ByteBuffer> consumer) {
        read(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests {@code n} bytes and accepts a {@link Consumer} with them (in a {@link ByteBuffer}) (with the
     * specified {@link ByteOrder}) once received.
     * <br><br>
     * If the amount of bytes requested already reside in the buffer, then this method may block to accept the
     * {@link Consumer} with the bytes. Otherwise, it simply queues up a request for the bytes, which does not block.
     * <br><br>
     * If encryption is active with a {@link Cipher} that uses padding, then this method should <strong>not</strong> be
     * called directly, as each grouping of bytes ({@code byte}, {@code short}, {@code int}, etc.) is encrypted
     * separately and will most-likely not reflect the amount of bytes requested.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @param order    The byte order of the data being received.
     */
    default void read(int n, Consumer<ByteBuffer> consumer, ByteOrder order) {
        readUntil(n, buffer -> {
            consumer.accept(buffer);
            return false;
        }, order);
    }
    
    /**
     * Calls {@link #read(int, Consumer, ByteOrder)}; however, once finished, {@link #read(int, Consumer, ByteOrder)} is
     * called once again with the same parameters; this loops until the specified {@link Predicate} returns {@code
     * false}, whereas {@link #read(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n         The amount of bytes requested.
     * @param predicate Holds the operations that should be performed once the {@code n} bytes are received.
     * @param order     The byte order of the data being received.
     */
    void readUntil(int n, Predicate<ByteBuffer> predicate, ByteOrder order);
    
    /**
     * Calls {@link #read(int, Consumer, ByteOrder)}; however, once finished, {@link #read(int, Consumer, ByteOrder)} is
     * called once again with the same parameters; this loops indefinitely, whereas
     * {@link #read(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of bytes requested.
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @param order    The byte order of the data being received.
     */
    default void readAlways(int n, Consumer<ByteBuffer> consumer, ByteOrder order) {
        readUntil(n, buffer -> {
            consumer.accept(buffer);
            return true;
        }, order);
    }
}
