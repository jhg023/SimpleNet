package packets;

import simplenet.packet.incoming.*;

import java.nio.*;

public final class IntPacket implements IncomingPacket {

	@Override
	public void read(ByteBuffer payload) {
		System.out.println("INT PACKET: " + payload.getInt());
	}

}
