package main;

import simplenet.Client;
import simplenet.packet.Packet;

public final class Main {

	public static void main(String[] args) {
        // Instantiate a new Client.
        var client = new Client();

        // Register one connection listener.
        client.onConnect(() -> {
            System.out.println(client + " has connected to the server!");

            // Builds a packet and sends it to the server immediately.
            Packet.builder()
                  .putByte(1)
                  .putInt(42)
                  .writeAndFlush(client);
        });

        // Register one disconnection listener.
        client.onDisconnect(() -> System.out.println(client + " has disconnected from the server!"));

        // Attempt to connect to a server.
        client.connect("localhost", 43594);

		while (true) {
			Thread.onSpinWait();
		}
	}

}
