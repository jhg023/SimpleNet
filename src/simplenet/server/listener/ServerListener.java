package simplenet.server.listener;

import simplenet.*;
import simplenet.client.*;
import simplenet.packet.*;
import simplenet.server.*;

import java.nio.channels.*;

/**
 * The {@link CompletionHandler} that is executed when the
 * {@link Server} receives a connection from a {@link Client}.
 * <p>
 * If the connection is accepted, then attempt to asynchronously
 * read a {@link Packet} from a {@link Client}; otherwise, print the stacktrace.
 *
 * @author Jacob G.
 * @since October 22, 2017
 */
public final class ServerListener extends Listener<AsynchronousSocketChannel, Server> {

	@Override
	protected void onCompletion(AsynchronousSocketChannel channel, Server server) {
		server.getConsumer().accept(channel);

		/*
		 * Asynchronously accept future connections.
		 */
		server.getChannel().accept(server, this);
	}

	@Override
	protected AsynchronousSocketChannel getChannel(AsynchronousSocketChannel channel, Server server) {
		return channel;
	}

}
