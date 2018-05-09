package main;

import simplenet.client.*;
import simplenet.packet.Packet;

public final class Main {

	public static void main(String[] args) {
	    // Instantiate a new Client.
		Client client = new Client();

		// Register one connection and disconnection listener.
		client.onConnect(channel -> System.out.println(channel + " has connected to the server!"));
        client.onDisconnect(channel -> System.out.println(channel + " has disconnected from the server!"));

		// Attempt to connect to a server.
		client.connect("localhost", 43_594);

		byte[] b = "Hello World!".getBytes();

		// Builds a packet and sends it to the server immediately.
        Packet.builder().putLong(2135).writeAndFlush(client);
        Packet.builder().putByte(b.length).putBytes(b).writeAndFlush(client);

		while (true) {
			Thread.onSpinWait();
		}
	}

}
