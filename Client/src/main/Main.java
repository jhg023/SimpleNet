package main;

import simplenet.client.*;
import simplenet.packet.Packet;

public final class Main {

	public static void main(String[] args) {
        // Instantiate a new Client.
        var client = new Client();

        // Attempt to connect to a server.
        client.connect("localhost", 43594);

        // Register one connection listener.
        client.onConnect(channel -> {
            System.out.println(channel + " has connected to the server!");

            // Builds a packet and sends it to the server immediately.
            Packet.builder().putByte(1).putInt(42).writeAndFlush(client);
        });

        // Register one disconnection listener.
        client.onDisconnect(channel -> System.out.println(channel + " has disconnected from the server!"));

		while (true) {
			Thread.onSpinWait();
		}
	}

}
