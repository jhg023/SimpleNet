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

/**
 * A wrapper object that represents a mutable, primitive {@code boolean}.
 *
 * @author Jacob G.
 * @since May 21, 2019
 */
public final class MutableBoolean {
    
    /**
     * The backing {@code boolean} of this {@link MutableBoolean}.
     */
    private boolean value;
    
    /**
     * Constructs a new {@link MutableBoolean} with initial value {@code false}.
     */
    public MutableBoolean() {
        this(false);
    }
    
    /**
     * Constructs a new {@link MutableBoolean} with initial value {@code value}.
     *
     * @param value The initial value of this {@link MutableBoolean}.
     */
    public MutableBoolean(boolean value) {
        this.value = value;
    }
    
    /**
     * Gets the value of this {@link MutableBoolean} as a {@code boolean}.
     *
     * @return The value of this {@link MutableBoolean}.
     */
    public boolean get() {
        return value;
    }
    
    /**
     * Sets the value of this {@link MutableBoolean} to a specified {@code boolean} value.
     *
     * @param value The value to set this {@link MutableBoolean} to.
     * @return This {@link MutableBoolean} for the convenience of method-chaining.
     */
    public MutableBoolean set(boolean value) {
        this.value = value;
        return this;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MutableBoolean)) {
            return false;
        }
        
        return ((MutableBoolean) o).value == value;
    }
    
    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }
    
    @Override
    public String toString() {
        return "MutableBoolean[value = " + value + "]";
    }
}
