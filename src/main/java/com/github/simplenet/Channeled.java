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
package com.github.simplenet;

import java.io.IOException;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.Channel;

/**
 * An {@code interface} that denotes an entity as having a backing {@link Channel}.
 *
 * @author Jacob G.
 * @since November 6, 2017
 */
@FunctionalInterface
interface Channeled<T extends AsynchronousChannel> {

    /**
     * Gets the backing {@link Channel} of this entity.
     *
     * @return An {@link T}.
     */
    T getChannel();

    /**
     * Closes the backing {@link Channel} of this entity, which results in the firing of any disconnect-listeners
     * that exist.
     */
    default void close() {
        try {
            getChannel().close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to close the backing AsynchronousChannel:", e);
        }
    }
}
