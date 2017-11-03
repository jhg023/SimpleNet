package packets;

import simplenet.packet.incoming.*;

import java.nio.*;

public final class FloatPacket implements IncomingPacket {

	@Override
	public void read(ByteBuffer payload) {
		System.out.println("FLOAT PACKET: " + payload.getFloat());
	}

}
