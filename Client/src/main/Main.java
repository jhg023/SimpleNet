package main;

import simplenet.client.*;
import simplenet.packet.Packet;

public final class Main {

	public static void main(String[] args) {
		Client client = new Client();

		client.connect("localhost", 43_594);

        Packet.builder().putByte(1).putInt(42).writeAndFlush(client);

		while (true) {
			Thread.onSpinWait();
		}
	}

}
