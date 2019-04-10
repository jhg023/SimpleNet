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
package simplenet.utility.data;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An interface that defines the methods required to read {@link String}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface StringReader extends ShortReader {

    /**
     * Reads a {@link String} with {@link StandardCharsets#UTF_8} as the encoding and {@link ByteOrder#BIG_ENDIAN} as
     * the {@code order}, but blocks the executing thread unlike {@link #readString(Consumer)}.
     *
     * @return A {@link String}.
     * @throws IllegalStateException if this method is called inside of a non-blocking callback.
     * @see #readString(Charset)
     */
    default String readString() throws IllegalStateException {
        return readString(StandardCharsets.UTF_8);
    }

    /**
     * Reads a {@link String} with the specified {@link Charset} and {@link ByteOrder#BIG_ENDIAN} as the
     * {@code order}, but blocks the executing thread unlike {@link #readString(Consumer)}.
     *
     * @return A {@link String}.
     * @throws IllegalStateException if this method is called inside of a non-blocking callback.
     * @see #readString(Charset, ByteOrder)
     */
    default String readString(Charset charset) throws IllegalStateException {
        return readString(charset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads a {@link String} with the specified {@link Charset} and {@link ByteOrder}, but blocks the executing
     * thread unlike {@link #readString(Consumer)}.
     *
     * @return A {@link String}.
     * @throws IllegalStateException if this method is called inside of a non-blocking callback.
     */
    default String readString(Charset charset, ByteOrder order) throws IllegalStateException {
        blockingInsideCallback();
        CompletableFuture<String> future = new CompletableFuture<String>();
        readString(future::complete, charset, order);
        return read(future);
    }

    /**
     * Calls {@link #readString(Consumer, Charset, ByteOrder)} with {@link StandardCharsets#UTF_8} as the encoding and
     * {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @see #readString(Consumer, Charset, ByteOrder)
     */
    default void readString(Consumer<String> consumer) {
        readString(consumer, StandardCharsets.UTF_8, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Calls {@link #readString(Consumer, Charset, ByteOrder)} with the specified {@link Charset} as the encoding and
     * {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @see #readString(Consumer, Charset, ByteOrder)
     */
    default void readString(Consumer<String> consumer, Charset charset) {
        readString(consumer, charset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Requests a single {@link String}, with the specified {@link Charset} and {@link ByteOrder}, and accepts a
     * {@link Consumer} with the {@link String} when it is received.
     * <br><br>
     * A {@code short} is used to store the length of the {@link String} in the payload header, which imposes a
     * maximum {@link String} length of {@code 65,535} with a {@link StandardCharsets#UTF_8} encoding or
     * {@code 32,767} (or less) with a different encoding.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @param charset  The {@link Charset} encoding of the {@link String}.
     * @param order    The byte order of the data being received.
     */
    default void readString(Consumer<String> consumer, Charset charset, ByteOrder order) {
        readShort(length -> processBytes(length, consumer, charset, order));
    }

    /**
     * Calls {@link #readStringAlways(Consumer, Charset, ByteOrder)} with {@link StandardCharsets#UTF_8} as the encoding
     * and {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @see #readStringAlways(Consumer, Charset, ByteOrder)
     */
    default void readStringAlways(Consumer<String> consumer) {
        readStringAlways(consumer, StandardCharsets.UTF_8, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Calls {@link #readStringAlways(Consumer, Charset, ByteOrder)} with the specified {@link Charset} as the encoding
     * and {@link ByteOrder#BIG_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @see #readStringAlways(Consumer, Charset, ByteOrder)
     */
    default void readStringAlways(Consumer<String> consumer, Charset charset) {
        readStringAlways(consumer, charset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Calls {@link #readString(Consumer, Charset, ByteOrder)}; however, once finished,
     * {@link #readString(Consumer, Charset, ByteOrder)} is called once again with the same consumer; this method loops
     * indefinitely, whereas {@link #readString(Consumer, Charset, ByteOrder)} completes after a single iteration.
     * <br><br>
     * A {@code short} is used to store the length of the {@link String} in the payload header, which imposes a
     * maximum {@link String} length of {@code 65,535} with a {@link StandardCharsets#UTF_8} encoding or
     * {@code 32,767} with a {@link StandardCharsets#UTF_16} encoding.
     *
     * @param consumer Holds the operations that should be performed once the {@link String} is received.
     * @param charset  The {@link Charset} encoding of the {@link String}.
     * @param order    The byte order of the data being received.
     */
    default void readStringAlways(Consumer<String> consumer, Charset charset, ByteOrder order) {
        readShortAlways(length -> processBytes(length, consumer, charset, order));
    }

    /**
     * A helper method to eliminate duplicate code.
     *
     * @param n        The amount of bytes requested (the length of the {@link String}).
     * @param consumer Holds the operations that should be performed once the {@code n} bytes are received.
     * @param charset  The {@link Charset} encoding of the {@link String}.
     */
    default void processBytes(short n, Consumer<String> consumer, Charset charset, ByteOrder order) {
        int length = order == ByteOrder.LITTLE_ENDIAN ? Short.reverseBytes(n) : n;

        read(Byte.BYTES * (length & 0xFFFF), buffer -> {
            byte[] b = new byte[length];
            buffer.get(b);
            consumer.accept(new String(b, charset));
        }, order);
    }

}
