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
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * An interface that defines the methods required to read {@code int}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface IntReader extends DataReader {
    
    /**
     * Calls {@link #readInt(IntConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code int} is received.
     * @see #readInt(IntConsumer, ByteOrder)
     */
    default void readInt(IntConsumer consumer) {
        readInt(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code int}, with the specified {@link ByteOrder}, and accepts a {@link IntConsumer} with
     * the {@code int} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code int} is received.
     * @param order    The byte order of the data being received.
     */
    default void readInt(IntConsumer consumer, ByteOrder order) {
        read(Integer.BYTES, buffer -> consumer.accept(buffer.getInt()), order);
    }
    
    /**
     * Calls {@link #readInt(IntConsumer)}; however, once finished, {@link #readInt(IntConsumer)} is
     * called once again with the same consumer; this method loops until the specified {@link IntPredicate}
     * returns {@code false}, whereas {@link #readInt(IntConsumer)} completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code int} is received.
     */
    default void readIntUntil(IntPredicate predicate) {
        readIntUntil(predicate, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readInt(IntConsumer, ByteOrder)}; however, once finished,
     * {@link #readInt(IntConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * until the specified {@link IntPredicate} returns {@code false}, whereas
     * {@link #readInt(IntConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code int} is received.
     * @param order     The byte order of the data being received.
     */
    default void readIntUntil(IntPredicate predicate, ByteOrder order) {
        readUntil(Integer.BYTES, buffer -> predicate.test(buffer.getInt()), order);
    }
    
    /**
     * Calls {@link #readIntAlways(IntConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code int} is received.
     * @see #readIntAlways(IntConsumer, ByteOrder)
     */
    default void readIntAlways(IntConsumer consumer) {
        readIntAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readInt(IntConsumer, ByteOrder)}; however, once finished,
     * {@link #readInt(IntConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readInt(IntConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code int} is received.
     * @param order    The byte order of the data being received.
     */
    default void readIntAlways(IntConsumer consumer, ByteOrder order) {
        readAlways(Integer.BYTES, buffer -> consumer.accept(buffer.getInt()), order);
    }
    
    /**
     * Calls {@link #readInts(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code int}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code int}s are received.
     * @see #readInts(int, Consumer, ByteOrder)
     */
    default void readInts(int n, Consumer<int[]> consumer) {
        readInts(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code int[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code int}s are received.
     *
     * @param n        The amount of {@code int}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code int}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readInts(int n, Consumer<int[]> consumer, ByteOrder order) {
        read(Integer.BYTES * n, buffer -> processInts(buffer, n, consumer), order);
    }
    
    /**
     * Calls {@link #readIntsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code int}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code int}s are received.
     */
    default void readIntsAlways(int n, Consumer<int[]> consumer) {
        readIntsAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readInts(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readInts(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readInts(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code int}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code int}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readIntsAlways(int n, Consumer<int[]> consumer, ByteOrder order) {
        readAlways(Integer.BYTES * n, buffer -> processInts(buffer, n, consumer), order);
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link ByteBuffer} that contains the bytes needed to map to {@code int}s.
     * @param n          The amount of {@code int}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code int}s are received.
     */
    private void processInts(ByteBuffer buffer, int n, Consumer<int[]> consumer) {
        var i = new int[n];
        buffer.asIntBuffer().get(i);
        consumer.accept(i);
    }
}
