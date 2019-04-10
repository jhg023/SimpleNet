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
package simplenet.utility;

/**
 * A wrapper object that represents a mutable, primitive {@code int}.
 *
 * @author Jacob G.
 * @since February 19, 2019
 */
public final class MutableInt {

    /**
     * The backing {@code int} of this {@link MutableInt}.
     */
    private int value;

    /**
     * Constructs a new {@link MutableInt} with initial value {@code 0}.
     */
    public MutableInt() {
        this(0);
    }

    /**
     * Constructs a new {@link MutableInt} with initial value {@code value}.
     *
     * @param value The initial value of this {@link MutableInt}.
     */
    public MutableInt(int value) {
        this.value = value;
    }

    /**
     * Adds a specified value to this {@link MutableInt}.
     *
     * @param value The value to add.
     * @return This {@link MutableInt} for the convenience of method-chaining.
     */
    public MutableInt add(int value) {
        this.value += value;
        return this;
    }

    /**
     * Gets the value of this {@link MutableInt} as an {@code int}.
     *
     * @return The value of this {@link MutableInt}.
     */
    public int get() {
        return value;
    }

    /**
     * Sets the value of this {@link MutableInt} to a specified {@code int} value.
     *
     * @param value The value to set this {@link MutableInt} to.
     * @return This {@link MutableInt} for the convenience of method-chaining.
     */
    public MutableInt set(int value) {
        this.value = value;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MutableInt)) {
            return false;
        }

        return ((MutableInt) o).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "MutableInt[value = " + value + "]";
    }

}
