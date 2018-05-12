package simplenet.client.listener;

import simplenet.*;
import simplenet.client.*;
import simplenet.server.*;
import simplenet.server.listener.ServerListener;

import java.io.IOException;
import java.nio.channels.*;

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
        client.getConnectionListeners().forEach(consumer -> consumer.accept(client));

        var channel = client.getChannel();

        channel.read(client.getBuffer(), client, ServerListener.getInstance());
    }

    @Override
    public void failed(Throwable t, Client client) {
        t.printStackTrace();
    }

}
