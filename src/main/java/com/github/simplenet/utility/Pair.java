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
package com.github.simplenet.utility;

import java.util.Objects;

/**
 * A class that acts as a {@link K}-{@link V} tuple.
 *
 * @author Jacob G.
 * @version October 20, 2019
 */
public final class Pair<K, V> {

    /**
     * The key of this {@link Pair}.
     */
    private final K key;

    /**
     * The value of this {@link Pair}.
     */
    private final V value;

    /**
     * Creates a new {@link Pair} with the specified key and value.
     *
     * @param key   the key.
     * @param value the value.
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key of this {@link Pair}.
     *
     * @return a {@link K}.
     */
    public K getKey() {
        return key;
    }

    /**
     * Gets the value of this {@link Pair}.
     *
     * @return a {@link V}.
     */
    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair<?, ?>)) {
            return false;
        }

        var pair = (Pair<?, ?>) o;

        return Objects.equals(key, pair.key) && Objects.equals(value, pair.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "Pair[key: " + key + ", value: " + value + "]";
    }
}
