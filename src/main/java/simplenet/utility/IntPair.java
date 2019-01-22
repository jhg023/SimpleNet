package simplenet.utility;

import java.util.Objects;

/**
 * A class that acts as an {@code int}-{@link T} tuple.
 *
 * @author Jacob G.
 * @version January 12, 2019
 */
public final class IntPair<T> {
    
    /**
     * The key of this {@link IntPair}.
     */
    private int key;
    
    /**
     * The value of this {@link IntPair}.
     */
    private T value;
    
    /**
     * Creates a new {@link IntPair} with the specified key and value.
     *
     * @param key   the key.
     * @param value the value.
     */
    public IntPair(int key, T value) {
        this.key = key;
        this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntPair<?>)) {
            return false;
        }
        
        var pair = (IntPair<?>) o;
        
        return key == pair.key && Objects.equals(value, pair.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
    
    @Override
    public String toString() {
        return "IntPair[key: " + key + ", value: " + value + "]";
    }
    
    /**
     * Gets this {@link IntPair}'s key.
     *
     * @return the key as an {@code int}.
     */
    public int getKey() {
        return key;
    }
    
    /**
     * Gets this {@link IntPair}'s value.
     *
     * @return the value as a {@link T}.
     */
    public T getValue() {
        return value;
    }
    
}