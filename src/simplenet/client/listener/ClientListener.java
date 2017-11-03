package simplenet.client.listener;

import simplenet.*;
import simplenet.client.*;
import simplenet.packet.*;
import simplenet.server.*;
import simplenet.utility.*;

import java.nio.channels.*;

/**
 * The {@link CompletionHandler} that is executed when a
 * {@link Client} connects to the {@link Server}.
 * <p>
 * If the connection is accepted, then attempt to asynchronously
 * read a packet from a {@link Server}; otherwise, print the stacktrace.
 *
 * @author Jacob G.
 * @since October 22, 2017
 */
public final class ClientListener extends Listener<Void, Tuple<Client, AsynchronousSocketChannel>> {

	@Override
	protected void onCompletion(Tuple<Client, AsynchronousSocketChannel> tuple) {
		System.out.println("\nClient connected successfully!");
	}

	@Override
	protected AsynchronousSocketChannel getChannel(Void result, Tuple<Client, AsynchronousSocketChannel> tuple) {
		return tuple.getRight();
	}

	@Override
	protected Packetable getPacketable(Tuple<Client, AsynchronousSocketChannel> tuple) {
		return tuple.getLeft();
	}

}
