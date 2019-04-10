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

import simplenet.utility.exposed.BooleanConsumer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An interface that defines the methods required to read {@code boolean}s over a network with SimpleNet.
 * <br><br>
 * {@code boolean}s are sent over the network as {@code byte}s with a value of {@code 1} for {@code true}
 * and a value of {@code 0} for {@code false}.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface BooleanReader extends DataReader {

    /**
     * Reads a {@code boolean} from the network, but blocks the executing thread unlike
     * {@link #readBoolean(BooleanConsumer)}.
     *
     * @return A {@code boolean}.
     * @throws IllegalStateException if this method is called inside of a non-blocking callback.
     * @see #readBoolean(BooleanConsumer)
     */
    default boolean readBoolean() throws IllegalStateException {
        blockingInsideCallback();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        readBoolean(future::complete);
        return read(future);
    }

    /**
     * Requests a single {@code boolean}, and accepts a {@link BooleanConsumer} with the {@code boolean} when it is
     * received.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     */
    default void readBoolean(BooleanConsumer consumer) {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.get() == 1), ByteOrder.BIG_ENDIAN);
    }

    /**
     * Calls {@link #readBoolean(BooleanConsumer)}; however, once finished, {@link #readBoolean(BooleanConsumer)} is
     * called once again with the same consumer; this method loops indefinitely, whereas
     * {@link #readBoolean(BooleanConsumer)} completes after a single iteration.
     *
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.
     */
    default void readBooleanAlways(BooleanConsumer consumer) {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.get() == 1), ByteOrder.BIG_ENDIAN);
    }

    /**
     * Requests a {@code boolean[]} of length {@code n} and accepts a {@link Consumer} when all of the
     * {@code boolean}s are received.
     *
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     */
    default void readBooleans(int n, Consumer<boolean[]> consumer) {
        read(Byte.BYTES * n, buffer -> processBooleans(buffer, n, consumer), ByteOrder.BIG_ENDIAN);
    }

    /**
     * Calls {@link #readBooleans(int, Consumer)}; however, once finished, {@link #readBooleans(int, Consumer)} is
     * called once again with the same parameter; this loops indefinitely, whereas
     * {@link #readBooleans(int, Consumer)} completes after a single iteration.
     *
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} compressed {@code boolean}s are
     *                 received.
     */
    default void readBooleansAlways(int n, Consumer<boolean[]> consumer) {
        readAlways(Byte.BYTES * n, buffer -> processBooleans(buffer, n, consumer), ByteOrder.BIG_ENDIAN);
    }

    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer   The {@link ByteBuffer} that contains the bytes needed to map to {@code boolean}s.
     * @param n        The amount of {@code boolean}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     */
    default void processBooleans(ByteBuffer buffer, int n, Consumer<boolean[]> consumer) {
        boolean[] b = new boolean[n];

        for (int i = 0; i < n; i++) {
            b[i] = buffer.get() == 1;
        }

        consumer.accept(b);
    }

}
