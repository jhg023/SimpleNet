package simplenet.server.listener;

import simplenet.*;
import simplenet.client.*;
import simplenet.packet.*;
import simplenet.server.*;
import simplenet.utility.*;

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
public final class ServerListener extends Listener<AsynchronousSocketChannel, Tuple<Server, AsynchronousServerSocketChannel>> {

	@Override
	protected void onCompletion(Tuple<Server, AsynchronousServerSocketChannel> tuple) {
		tuple.getRight().accept(tuple, this);
	}

	@Override
	protected AsynchronousSocketChannel getChannel(AsynchronousSocketChannel channel, Tuple<Server, AsynchronousServerSocketChannel> tuple) {
		return channel;
	}

	@Override
	protected Packetable getPacketable(Tuple<Server, AsynchronousServerSocketChannel> tuple) {
		return tuple.getLeft();
	}

}
