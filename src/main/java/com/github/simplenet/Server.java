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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The entity that all {@link Client}s will connect to.
 *
 * @author Jacob G.
 * @since November 1, 2017
 */
public class Server implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    /**
     * The maximum value for a port that a {@link Server server} can be bound to.
     */
    private static final int MAX_PORT = 65_535;

    /**
     * The backing {@link Channel channel} of this {@link Server server}.
     */
    private ServerSocketChannel channel;

    /**
     * A thread-safe {@link Set} that keeps track of {@link Client clients} connected to this {@link Server server}.
     */
    private final Set<Client> connectedClients;

    /**
     * A collection of listeners that are fired when a {@link Client client} connects to this {@link Server server}.
     */
    private final Collection<Consumer<Client>> connectListeners;

    /**
     * Instantiates a new {@link Server}.
     */
    public Server() {
        this.connectedClients = ConcurrentHashMap.newKeySet();
        this.connectListeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Attempts to bind the {@link Server} to the specified {@code address} and {@code port}.
     *
     * @param address    The IP address to bind to, whose value can also be {@code "localhost"}.
     * @param port       The port to bind to, which must be in the range: {@code 0 <= port <= 65535}.
     * @throws IllegalArgumentException If {@code port} is less than 0 or greater than 65535.
     * @throws IllegalStateException    If this server is already running on any address/port.
     * @throws IllegalStateException    If the server is unable to bind to the specified address or port.
     */
    public final void bind(String address, int port) {
        Objects.requireNonNull(address);

        if (port < 0 || port > MAX_PORT) {
            throw new IllegalArgumentException("The port must be between 0 and 65535!");
        }

        try {
            channel = ServerSocketChannel.open();
        } catch (IOException e) {
            throw new UncheckedIOException("An IOException occurred when attempting to open the server!", e);
        }

        try {
            channel.configureBlocking(true);
        } catch (IOException e) {
            throw new UncheckedIOException("An IOException occurred when configuring the channel to block!", e);
        }

        try {
            channel.bind(new InetSocketAddress(address, port));
        } catch (IOException e) {
            throw new UncheckedIOException("An IOException occurred when attempting to bind the server to the " +
                "specified address and port!", e);
        }

        var virtualThreadFactory = Thread.builder().daemon(false).virtual().name("SimpleNet-", 1).factory();

        // Use a kernel thread to accept connections, and virtual threads for everything else.
        Thread.builder().daemon(false).name("SimpleNet").task(() -> {
            while (true) {
                try {
                    SocketChannel connection = channel.accept();

                    virtualThreadFactory.newThread(() -> {
                        Client client = new Client(connection);
                        connectedClients.add(client);
                        client.onDisconnect(() -> connectedClients.remove(client));
                        connectListeners.forEach(consumer -> consumer.accept(client));
                    }).start();
                } catch (IOException e) {
                    LOGGER.error("An IOException occurred when attempting to accept a client connection!", e);
                    break;
                }
            }
        }).start();
    }
    
    /**
     * Closes this {@link Server} by first invoking {@link Client#close()} on every connected {@link Client}, and
     * then closes the backing {@link ServerSocketChannel}.
     */
    @Override
    public final void close() {
        connectedClients.removeIf((Client client) -> {
            client.close();
            return true;
        });

        try {
            channel.close();
        } catch (IOException e) {
            LOGGER.error("An IOException occurred when attempting to close the backing channel!", e);
        }
    }

    /**
     * Registers a listener that fires when a {@link Client client} connects to this {@link Server server}.
     * <br><br>
     * When invoking this method more than once, multiple listeners will be registered.
     *
     * @param listener A {@link Consumer} that will be accepted when a {@link Client client} disconnects from this
     *                 {@link Server server}.
     */
    public final void onConnect(Consumer<Client> listener) {
        connectListeners.add(listener);
    }

    /**
     * @return Gets an unmodifiable {@link Set} of {@link Client clients} connected to this {@link Server}.
     */
    public final Set<Client> getConnectedClients() {
        return Set.copyOf(connectedClients);
    }
}
