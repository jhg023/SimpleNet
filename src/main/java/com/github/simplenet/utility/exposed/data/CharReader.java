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

import com.github.simplenet.utility.exposed.consumer.CharConsumer;
import com.github.simplenet.utility.exposed.predicate.CharPredicate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

/**
 * An interface that defines the methods required to read {@code char}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface CharReader extends DataReader {
    
    /**
     * Calls {@link #readChar(CharConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code char} is received.
     * @see #readChar(CharConsumer, ByteOrder)
     */
    default void readChar(CharConsumer consumer) {
        readChar(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a single {@code char}, with the specified {@link ByteOrder}, and accepts a {@link CharConsumer} with
     * the {@code char} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code char} is received.
     * @param order    The byte order of the data being received.
     */
    default void readChar(CharConsumer consumer, ByteOrder order) {
        read(Character.BYTES, buffer -> consumer.accept(buffer.getChar()), order);
    }
    
    /**
     * Calls {@link #readChar(CharConsumer)}; however, once finished, {@link #readChar(CharConsumer)} is
     * called once again with the same consumer; this method loops until the specified {@link CharPredicate}
     * returns {@code false}, whereas {@link #readChar(CharConsumer)} completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code char} is received.
     */
    default void readCharUntil(CharPredicate predicate) {
        readCharUntil(predicate, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readChar(CharConsumer, ByteOrder)}; however, once finished,
     * {@link #readChar(CharConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * until the specified {@link CharPredicate} returns {@code false}, whereas {@link #readChar(CharConsumer, ByteOrder)}
     * completes after a single iteration.
     *
     * @param predicate Holds the operations that should be performed once the {@code char} is received.
     * @param order     The byte order of the data being received.
     */
    default void readCharUntil(CharPredicate predicate, ByteOrder order) {
        readUntil(Character.BYTES, buffer -> predicate.test(buffer.getChar()), order);
    }
    
    /**
     * Calls {@link #readCharAlways(CharConsumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code char} is received.
     * @see #readCharAlways(CharConsumer, ByteOrder)
     */
    default void readCharAlways(CharConsumer consumer) {
        readCharAlways(consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readChar(CharConsumer, ByteOrder)}; however, once finished,
     * {@link #readChar(CharConsumer, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readChar(CharConsumer, ByteOrder)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code char} is received.
     * @param order    The byte order of the data being received.
     */
    default void readCharAlways(CharConsumer consumer, ByteOrder order) {
        readAlways(Character.BYTES, buffer -> consumer.accept(buffer.getChar()), order);
    }
    
    /**
     * Calls {@link #readChars(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code char}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code char}s are received.
     * @see #readChars(int, Consumer, ByteOrder)
     */
    default void readChars(int n, Consumer<char[]> consumer) {
        readChars(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Requests a {@code char[]} of length {@code n} in the specified {@link ByteOrder} and accepts a {@link Consumer}
     * when all of the {@code char}s are received.
     *
     * @param n        The amount of {@code char}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code char}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readChars(int n, Consumer<char[]> consumer, ByteOrder order) {
        read(Character.BYTES * n, buffer -> processChars(buffer, n, consumer), order);
    }
    
    /**
     * Calls {@link #readCharsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code char}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code char}s are received.
     */
    default void readCharsAlways(int n, Consumer<char[]> consumer) {
        readCharsAlways(n, consumer, ByteOrder.BIG_ENDIAN);
    }
    
    /**
     * Calls {@link #readChars(int, Consumer, ByteOrder)}; however, once finished,
     * {@link #readChars(int, Consumer, ByteOrder)} is called once again with the same parameter; this loops
     * indefinitely, whereas {@link #readChars(int, Consumer, ByteOrder)} completes after a single iteration.
     *
     * @param n        The amount of {@code char}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code char}s are received.
     * @param order    The byte order of the data being received.
     */
    default void readCharsAlways(int n, Consumer<char[]> consumer, ByteOrder order) {
        readAlways(Character.BYTES * n, buffer -> processChars(buffer, n, consumer), order);
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link ByteBuffer} that contains the bytes needed to map to {@code char}s.
     * @param n          The amount of {@code char}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code char}s are received.
     */
    private void processChars(ByteBuffer buffer, int n, Consumer<char[]> consumer) {
        var c = new char[n];
        buffer.asCharBuffer().get(c);
        consumer.accept(c);
    }
}
