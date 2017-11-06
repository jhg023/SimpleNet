package main;

import simplenet.client.*;
import simplenet.packet.outgoing.*;

public class Main {

	public static void main(String[] args) {
		Client client = new Client();

		client.connect("localhost", 43594);

		new OutgoingPacket(0).putString("Hello World!").send(client);

		while (true) {
			Thread.onSpinWait();
		}
	}

}
