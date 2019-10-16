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
package com.github.simplenet;

import java.util.ArrayList;
import java.util.Collection;

abstract class Receiver<T> {

    /**
     * The size of this {@link Receiver}'s buffer.
     */
    final int bufferSize;

    /**
     * Listeners that are fired when a {@link Client} connects
     * to a {@link Server}.
     */
    final Collection<T> connectListeners;

    /**
     * Listeners that are fired right before a {@link Client} disconnects
     * from a {@link Server}.
     */
    final Collection<T> preDisconnectListeners;

    /**
     * Listeners that are fired right after a {@link Client} disconnects
     * from a {@link Server}.
     */
    final Collection<T> postDisconnectListeners;

    /**
     * Instantiates a new {@link Receiver} with a buffer capacity of {@code bufferSize}.
     *
     * @param bufferSize The capacity of the buffer used for reading in bytes.
     */
    Receiver(int bufferSize) {
        this.bufferSize = bufferSize;
        this.connectListeners = new ArrayList<>();
        this.preDisconnectListeners = new ArrayList<>();
        this.postDisconnectListeners = new ArrayList<>();
    }
    
    /**
     * Instantiates a new {@link Receiver} from an existing {@link Receiver}.
     *
     * @param receiver The existing {@link Receiver}.
     * @param <U> A {@link Receiver} or one of its children.
     */
    <U extends Receiver<T>> Receiver(U receiver) {
        this.bufferSize = receiver.bufferSize;
        this.connectListeners = receiver.connectListeners;
        this.preDisconnectListeners = receiver.preDisconnectListeners;
        this.postDisconnectListeners = receiver.postDisconnectListeners;
    }

    /**
     * Registers a listener that fires when a {@link Client} connects to a {@link Server}.
     * <br><br>
     * When calling this method more than once, multiple listeners are registered.
     *
     * @param listener A {@link T}.
     */
    public final void onConnect(T listener) {
        connectListeners.add(listener);
    }

    /**
     * Gets the buffer size of this {@link Receiver} in bytes.
     *
     * @return An {@code int}
     */
    public final int getBufferSize() {
        return bufferSize;
    }
}
