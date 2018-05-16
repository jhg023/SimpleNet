package simplenet.client.listener;

import java.nio.channels.CompletionHandler;
import simplenet.Listener;
import simplenet.client.Client;
import simplenet.server.Server;

/**
 * The {@link CompletionHandler} that is executed when a
 * {@link Client} connects to a {@link Server}.
 * <p>
 * If the connection is accepted, then attempt to asynchronously
 * read a packet from a {@link Server}; otherwise, print the stacktrace.
 *
 * @author Jacob G.
 * @since October 22, 2017
 */
public final class ClientListener implements CompletionHandler<Void, Client> {

    @Override
    public void completed(Void result, Client client) {
        client.getConnectionListeners().forEach(Runnable::run);

        client.getChannel().read(client.getBuffer(), client, Listener.getInstance());
    }

    @Override
    public void failed(Throwable t, Client client) {

    }

}
