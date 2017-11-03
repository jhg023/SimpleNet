package packets;

import simplenet.packet.incoming.*;

import java.nio.*;

public final class LongPacket implements IncomingPacket {

	@Override
	public void read(ByteBuffer payload) {
		System.out.println("LONG PACKET: " + payload.getLong());
	}

}
