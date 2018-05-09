package simplenet.client.listener;

import simplenet.*;
import simplenet.client.*;
import simplenet.server.*;

import java.nio.channels.*;

/**
 * The {@link Listener} that is executed when a
 * {@link Client} connects to the {@link Server}.
 * <p>
 * If the connection is accepted, then attempt to asynchronously
 * read a packet from a {@link Server}; otherwise, print the stacktrace.
 *
 * @author Jacob G.
 * @since October 22, 2017
 */
public final class ClientListener extends Listener<Void, Client> {

    @Override
    protected void onCompletion(Void result, Client client) {
        client.getConnectionListeners().forEach(consumer -> consumer.accept(client.getChannel()));
    }

	@Override
	protected AsynchronousSocketChannel getChannel(Void result, Client client) {
		return client.getChannel();
	}

}
