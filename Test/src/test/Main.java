package test;

import packets.*;
import simplenet.client.*;
import simplenet.packet.outgoing.*;
import simplenet.server.*;

import java.io.*;

public class Main {

	public static void main(String[] args) {
		Server server = new Server(channel -> {
			try {
				System.out.println(channel.getRemoteAddress() + " connected successfully!\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		server.bind("localhost", 43594);
		server.register(0, new BytePacket());
		server.register(1, new ShortPacket());
		server.register(2, new IntPacket());
		server.register(3, new LongPacket());
		server.register(4, new FloatPacket());

		Client client = new Client();
		client.connect("localhost", 43594);

		new OutgoingPacket(0).putByte(254).send(client);
		new OutgoingPacket(1).putShort(254).send(client);
		new OutgoingPacket(2).putInt(512).send(client);
		new OutgoingPacket(3).putLong(1024).send(client);
		new OutgoingPacket(4).putFloat(2048.5F).send(client);

		synchronized (Main.class) {
			try {
				Main.class.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
