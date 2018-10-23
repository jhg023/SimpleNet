package simplenet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import simplenet.channel.Channeled;
import simplenet.receiver.Receiver;

/**
 * The entity that all {@link Client}s will connect to.
 *
 * @author Jacob G.
 * @since November 1, 2017
 */
public final class Server extends Receiver<Consumer<Client>> implements Channeled<AsynchronousServerSocketChannel> {

    /**
     * The backing {@link ThreadPoolExecutor} used for I/O.
     */
    private final ThreadPoolExecutor executor;

    /**
     * The backing {@link Channel} of the {@link Server}.
     */
    private final AsynchronousServerSocketChannel channel;

    /**
     * Instantiates a new {@link Server} by attempting
     * to open the backing {@link AsynchronousServerSocketChannel}.
     *
     * @throws IllegalStateException If multiple {@link Server} instances are created.
     */
    public Server() {
        this(4096);
    }

    public Server(int bufferSize) {
        super(bufferSize);

        try {
            int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

            executor = new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), runnable -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(false);
                return thread;
            });

            executor.prestartAllCoreThreads();

            channel = AsynchronousServerSocketChannel.open(AsynchronousChannelGroup.withThreadPool(executor));
            channel.setOption(StandardSocketOptions.SO_RCVBUF, bufferSize);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open the channel!");
        }
    }

    /**
     * Attempts to bind the {@link Server} to a
     * specific {@code address} and {@code port}.
     *
     * @param address The IP address to bind to.
     * @param port    The port to bind to {@code 0 <= port <= 65535}.
     * @throws IllegalArgumentException If {@code port} is less than 0 or greater than 65535.
     * @throws AlreadyBoundException    If a server is already running on any address/port.
     * @throws RuntimeException         If the server is unable to be bound to a specific
     *                                  address or port.
     */
    public void bind(String address, int port) {
        Objects.requireNonNull(address);

        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("The port must be between 0 and 65535!");
        }

        try {
            channel.bind(new InetSocketAddress(address, port));

            final Client.Listener listener = new Client.Listener() {
                @Override
                public void failed(Throwable t, Client client) {
                    client.close();
                }
            };

            channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(AsynchronousSocketChannel channel, Void attachment) {
                    var client = new Client(bufferSize, channel);
                    connectListeners.forEach(consumer -> consumer.accept(client));
                    Server.this.channel.accept(null, this);
                    channel.read(client.getBuffer(), client, listener);
                }

                @Override
                public void failed(Throwable t, Void attachment) {

                }
            });

            System.out.println(String.format("Successfully bound to %s:%d!", address, port));
        } catch (AlreadyBoundException e) {
            throw new IllegalStateException("A server is already running!");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to bind the server!");
        }
    }

    @Override
    public void close() {
        Channeled.super.close();
        executor.shutdownNow();
    }

    /**
     * Gets the backing {@link Channel} of this {@link Server}.
     *
     * @return A {@link Channel}.
     */
    @Override
    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }

}
