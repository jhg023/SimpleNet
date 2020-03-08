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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

abstract class AbstractReceiver<T> {

    /**
     * The size of this {@link AbstractReceiver}'s buffer in bytes.
     */
    static final int BUFFER_SIZE = 8_192;

    /**
     * Listeners that are fired when a {@link Client} connects to a {@link Server}.
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
     * Instantiates a new {@link AbstractReceiver} with a buffer capacity of {@code bufferSize}.
     */
    AbstractReceiver() {
        this.connectListeners = new CopyOnWriteArrayList<>();
        this.preDisconnectListeners = new CopyOnWriteArrayList<>();
        this.postDisconnectListeners = new CopyOnWriteArrayList<>();
    }
    
    /**
     * Instantiates a new {@link AbstractReceiver} from an existing {@link AbstractReceiver}.
     *
     * @param receiver The existing {@link AbstractReceiver}.
     * @param <U> A {@link AbstractReceiver} or one of its children.
     */
    <U extends AbstractReceiver<T>> AbstractReceiver(U receiver) {
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
}
