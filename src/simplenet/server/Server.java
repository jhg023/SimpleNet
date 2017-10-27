package simplenet.server;

import simplenet.packet.incoming.*;
import simplenet.server.listener.*;
import simplenet.utility.*;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

public final class Server {

	private boolean bound;

	private final AsynchronousServerSocketChannel server;

	/**
	 * An array of {@link IncomingPacket}s received by
	 * the {@link Server}.
	 */
	private final IncomingPacket[] PACKETS = new IncomingPacket[255];

	private Server() {
		try {
			server = AsynchronousServerSocketChannel.open();
		} catch (IOException e) {
			throw new RuntimeException("Unable to start the server!");
		}
	}

	public void bind(String address, int port) {
		if (bound) {
			throw new IllegalStateException("A server is already running!!");
		}

		Objects.requireNonNull(address);

		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("The port must be between 0 and 65535!");
		}

		try {
			server.bind(new InetSocketAddress(address, port));

			bound = true;

			server.accept(new Tuple<>(this, server), new ServerListener());
		} catch (IOException e) {
			throw new RuntimeException("Unable to bind the server!");
		}
	}

	public Server register(int opcode, IncomingPacket packet) {
		if (PACKETS[opcode] != null) {
			throw new IllegalArgumentException(String.format("opcode %d has already been registered!", opcode));
		}

		PACKETS[opcode] = packet;

		return this;
	}

	public IncomingPacket[] getPackets() {
		return PACKETS;
	}

}
