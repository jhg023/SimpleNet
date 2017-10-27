package simplenet.server.listener;

import simplenet.client.*;
import simplenet.server.*;
import simplenet.utility.*;

import java.nio.*;
import java.nio.channels.*;

/**
 * The {@link CompletionHandler} that is executed when the
 * {@link Server} receives a connection from a {@link Client}.
 * <p>
 * If the connection is accepted, then attempt to asynchronously
 * read a packet from a {@link Client}; otherwise, print the stacktrace.
 *
 * @author Jacob G.
 * @since October 22, 2017
 */
public final class ServerListener implements CompletionHandler<AsynchronousSocketChannel, Tuple<Server, AsynchronousServerSocketChannel>> {

	private int currentOpcode = -1;

	private int currentLength = -1;

	/**
	 * Allocate a new {@link ByteBuffer} for the opcode
	 * and length, each being {@code 1} byte.
	 */
	private ByteBuffer buffer = ByteBuffer.allocate(2);

	@Override
	public void completed(AsynchronousSocketChannel client, Tuple<Server, AsynchronousServerSocketChannel> tuple) {
		AsynchronousServerSocketChannel channel = tuple.getRight();

		/*
		 * Asynchronously accept the next connection.
		 */
		channel.accept(new Tuple<>(tuple.getLeft(), channel), this);

		/*
		 * Handle this connection by continuously attempting
		 * to asynchronously read from the channel.
		 */
		client.read(buffer, buffer, new CompletionHandler<>() {
			@Override
			public void completed(Integer result, ByteBuffer buffer) {
				if (currentOpcode == -1 && buffer.hasRemaining()) {
					currentOpcode = buffer.get() & 0xFF;

					buffer.compact().clear();
				}

				if (currentLength == -1 && buffer.hasRemaining()) {
					currentLength = buffer.get() & 0xFF;

					if (currentLength > buffer.capacity()) {
						buffer = ByteBuffer.allocateDirect(currentLength).put(buffer);
					}
				}

				if (buffer.remaining() >= currentLength) {
					tuple.getLeft().getPackets()[currentOpcode].read(buffer);

					currentOpcode = currentLength = -1;

					buffer.compact().clear();

					client.read(buffer, buffer, this);
				}
			}

			@Override
			public void failed(Throwable t, ByteBuffer buffer) {
				t.printStackTrace();

				buffer.reset();
			}
		});
	}

	@Override
	public void failed(Throwable t, Tuple<Server, AsynchronousServerSocketChannel> pair) {
		t.printStackTrace();
	}

}
