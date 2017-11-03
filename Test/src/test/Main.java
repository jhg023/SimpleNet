package test;

import packets.*;
import simplenet.client.*;
import simplenet.packet.outgoing.*;
import simplenet.server.*;

public class Main {

	public static void main(String[] args) {
		Server server = new Server();
		server.bind("localhost", 43594);
		server.register(0, new BytePacket());
		server.register(1, new ShortPacket());
		server.register(2, new IntPacket());

		Client client = new Client();
		client.connect("localhost", 43594);

		new OutgoingPacket(0).putByte(254).send(client);
		new OutgoingPacket(1).putShort(254).send(client);
		new OutgoingPacket(2).putInt(512).send(client);

		synchronized (Main.class) {
			try {
				Main.class.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
