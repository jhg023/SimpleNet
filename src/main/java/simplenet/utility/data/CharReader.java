package simplenet.utility.data;

import bitbuffer.BitBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import simplenet.utility.exposed.CharConsumer;

/**
 * An interface that defines the methods required to read {@code char}s over a network with SimpleNet.
 *
 * @author Jacob G.
 * @version January 21, 2019
 */
public interface CharReader extends DataReader {
    
    /**
     * Reads a {@code char} with {@link ByteOrder#LITTLE_ENDIAN} order from the network, but blocks the executing thread
     * unlike {@link #readChar(CharConsumer)}.
     *
     * @return A {@code char}.
     * @see #readChar(ByteOrder)
     */
    default char readChar() {
        return readChar(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Reads a {@code char} with the specified {@link ByteOrder} from the network, but blocks the executing thread
     * unlike {@link #readChar(CharConsumer)}.
     *
     * @return A {@code char}.
     */
    default char readChar(ByteOrder order) {
        var future = new CompletableFuture<Character>();
        readChar(future::complete, order);
        return read(future);
    }
    
    /**
     * Calls {@link #readChar(CharConsumer, ByteOrder)} with {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code char} is received.
     * @see #readChar(CharConsumer, ByteOrder)
     */
    default void readChar(CharConsumer consumer) {
        readChar(consumer, ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Requests a single {@code char}, with the specified {@link ByteOrder}, and accepts a {@link CharConsumer} with
     * the {@code char} when it is received.
     *
     * @param consumer Holds the operations that should be performed once the {@code char} is received.
     * @param order    The byte order of the data being received.
     */
    default void readChar(CharConsumer consumer, ByteOrder order) {
        read(Character.SIZE, buffer -> consumer.accept(buffer.getChar(order)));
    }
    
    /**
     * Calls {@link #readCharAlways(CharConsumer, ByteOrder)} with {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param consumer Holds the operations that should be performed once the {@code char} is received.
     * @see #readCharAlways(CharConsumer, ByteOrder)
     */
    default void readCharAlways(CharConsumer consumer) {
        readCharAlways(consumer, ByteOrder.LITTLE_ENDIAN);
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
        readAlways(Character.SIZE, buffer -> consumer.accept(buffer.getChar(order)));
    }
    
    /**
     * Calls {@link #readChars(int, Consumer, ByteOrder)} with {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code char}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code char}s are received.
     * @see #readChars(int, Consumer, ByteOrder)
     */
    default void readChars(int n, Consumer<char[]> consumer) {
        readChars(n, consumer, ByteOrder.LITTLE_ENDIAN);
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
        read(Character.SIZE * n, buffer -> processChars(buffer, n, consumer, order));
    }
    
    /**
     * Calls {@link #readCharsAlways(int, Consumer, ByteOrder)} with {@link ByteOrder#LITTLE_ENDIAN} as the {@code order}.
     *
     * @param n        The amount of {@code char}s requested.
     * @param consumer Holds the operations that should be performed once the {@code n} {@code char}s are received.
     */
    default void readCharsAlways(int n, Consumer<char[]> consumer) {
        readCharsAlways(n, consumer, ByteOrder.LITTLE_ENDIAN);
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
        readAlways(Character.SIZE * n, buffer -> processChars(buffer, n, consumer, order));
    }
    
    /**
     * A helper method to eliminate duplicate code.
     *
     * @param buffer     The {@link BitBuffer} that contains the bits needed to map to {@code char}s.
     * @param n          The amount of {@code char}s requested.
     * @param consumer   Holds the operations that should be performed once the {@code n} {@code char}s are received.
     * @param order      The byte order of the {@code char}s being received.
     */
    private void processChars(BitBuffer buffer, int n, Consumer<char[]> consumer, ByteOrder order) {
        var c = new char[n];
        
        for (int i = 0; i < n; i++) {
            c[i] = buffer.getChar(order);
        }
        
        consumer.accept(c);
    }
    
}
