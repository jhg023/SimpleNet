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

import com.github.simplenet.utility.exposed.consumer.FloatConsumer;
import com.github.simplenet.utility.exposed.predicate.FloatPredicate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * An interface that defines the methods required to read {@code float}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface FloatReader extends DataReader {
    
    /**
     * Calls {@link #readFloat(FloatConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code float} is received.
     * @see #readFloat(FloatConsumer, ByteOrder)
     */
    default void readFloat(FloatConsumer consumer) {
        readFloat(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code float}, with the specified {@link ByteOrder}, and accepts a {@link FloatConsumer} with
     * the {@code float} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code float} is received.
     * @param order    The byte order of the data being received.
     */
    default void readFloat(FloatConsumer consumer, ByteOrder order) {
        read(Float.BYTES, buffer -> consumer.accept(buffer.getFloat()), order);
    }
    
    /**
     * Calls {@link #readFloat(FloatConsumer)}; however, once finished, {@link #readFloat(FloatConsumer)} is
     * called once again with the same consumer; this method loops until the specified {@link FloatPredicate}
     * returns {@code false}, whereas {@link #readFloat(FloatConsumer)} completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code float} is received.
     */
    default void readFloatUntil(FloatPredicate predicate) {
        readFloatUntil(predicate, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readFloat(FloatConsumer, ByteOrder)}; however, once finished,
     * {@link #readFloat(FloatConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * until the specified {@link FloatPredicate} returns {@code false}, whereas
     * {@link #readFloat(FloatConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code float} is received.
     * @param order     The byte order of the data being received.
     */
    default void readFloatUntil(FloatPredicate predicate, ByteOrder order) {
        readUntil(Float.BYTES, buffer -> predicate.test(buffer.getFloat()), order);
    }
    
    /**
     * Calls {@link #readFloatAlways(FloatConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code float} is received.
     * @see #readFloatAlways(FloatConsumer, ByteOrder)
     */
    default void readFloatAlways(FloatConsumer consumer) {
        readFloatAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readFloat(FloatConsumer, ByteOrder)}; however, once finished,
     * {@link #readFloat(FloatConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readFloat(FloatConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code float} is received.
     * @param order    The byte order of the data being received.
     */
    default void readFloatAlways(FloatConsumer consumer, ByteOrder order) {
        readAlways(Float.BYTES, buffer -> consumer.accept(buffer.getFloat()), order);
    }
    
    /**
     * Calls {@link #readFloats(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code float}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code float}s are received.
     * @see #readFloats(int, Consumer, ByteOrder)
     */
    default void readFloats(int n, Consumer<float[]> consumer) {
        readFloats(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code float[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code float}s are received.
     *
     * @param n        The amount of {@code float}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code float}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readFloats(int n, Consumer<float[]> consumer, ByteOrder order) {
        read(Float.BYTES * n, buffer -> processFloats(buffer, n, consumer), order);
    }
    
    /**
     * Calls {@link #readFloatsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code float}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code float}s are received.
     */
    default void readFloatsAlways(int n, Consumer<float[]> consumer) {
        readFloatsAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readFloats(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readFloats(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readFloats(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code float}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code float}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readFloatsAlways(int n, Consumer<float[]> consumer, ByteOrder order) {
        readAlways(Float.BYTES * n, buffer -> processFloats(buffer, n, consumer), order);
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link ByteBuffer} that contains the bytes needed to map to {@code float}s.
     * @param n          The amount of {@code float}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code float}s are received.
     */
    private void processFloats(ByteBuffer buffer, int n, Consumer<float[]> consumer) {
        var f = new float[n];
        buffer.asFloatBuffer().get(f);
        consumer.accept(f);
    }
}
