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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * An interface that defines the methods required to read {@code long}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface LongReader extends DataReader {
    
    /**
     * Calls {@link #readLong(LongConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code long} is received.
     * @see #readLong(LongConsumer, ByteOrder)
     */
    default void readLong(LongConsumer consumer) {
        readLong(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code long}, with the specified {@link ByteOrder}, and accepts a {@link LongConsumer} with
     * the {@code long} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code long} is received.
     * @param order    The byte order of the data being received.
     */
    default void readLong(LongConsumer consumer, ByteOrder order) {
        read(Long.BYTES, buffer -> consumer.accept(buffer.getLong()), order);
    }
    
    /**
     * Calls {@link #readLong(LongConsumer)}; however, once finished, {@link #readLong(LongConsumer)} is
     * called once again with the same consumer; this method loops until the specified {@link LongPredicate}
     * returns {@code false}, whereas {@link #readLong(LongConsumer)} completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code long} is received.
     */
    default void readLongUntil(LongPredicate predicate) {
        readLongUntil(predicate, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readLong(LongConsumer, ByteOrder)}; however, once finished,
     * {@link #readLong(LongConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * until the specified {@link LongPredicate} returns {@code false}, whereas
     * {@link #readLong(LongConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code long} is received.
     * @param order     The byte order of the data being received.
     */
    default void readLongUntil(LongPredicate predicate, ByteOrder order) {
        readUntil(Long.BYTES, buffer -> predicate.test(buffer.getLong()), order);
    }
    
    /**
     * Calls {@link #readLongAlways(LongConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code long} is received.
     * @see #readLongAlways(LongConsumer, ByteOrder)
     */
    default void readLongAlways(LongConsumer consumer) {
        readLongAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readLong(LongConsumer, ByteOrder)}; however, once finished,
     * {@link #readLong(LongConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readLong(LongConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code long} is received.
     * @param order    The byte order of the data being received.
     */
    default void readLongAlways(LongConsumer consumer, ByteOrder order) {
        readAlways(Long.BYTES, buffer -> consumer.accept(buffer.getLong()), order);
    }
    
    /**
     * Calls {@link #readLongs(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code long}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code long}s are received.
     * @see #readLongs(int, Consumer, ByteOrder)
     */
    default void readLongs(int n, Consumer<long[]> consumer) {
        readLongs(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code long[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code long}s are received.
     *
     * @param n        The amount of {@code long}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code long}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readLongs(int n, Consumer<long[]> consumer, ByteOrder order) {
        read(Long.BYTES * n, buffer -> processLongs(buffer, n, consumer), order);
    }
    
    /**
     * Calls {@link #readLongsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code long}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code long}s are received.
     */
    default void readLongsAlways(int n, Consumer<long[]> consumer) {
        readLongsAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readLongs(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readLongs(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readLongs(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code long}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code long}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readLongsAlways(int n, Consumer<long[]> consumer, ByteOrder order) {
        readAlways(Long.BYTES * n, buffer -> processLongs(buffer, n, consumer), order);
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link ByteBuffer} that contains the bytes needed to map to {@code long}s.
     * @param n          The amount of {@code long}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code long}s are received.
     */
    private void processLongs(ByteBuffer buffer, int n, Consumer<long[]> consumer) {
        var l = new long[n];
        buffer.asLongBuffer().get(l);
        consumer.accept(l);
    }
}
