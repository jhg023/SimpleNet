package simplenet.packet.outgoing;

import simplenet.packet.*;

import java.nio.*;

public abstract class OutgoingPacket implements Packet {

	/**
	 * The payload of the {@link Packet}
	 * containing data sent to/from the
	 * server.
	 */
	private ByteBuffer payload;

	/**
	 * Instantiates a raw {@link Packet}
	 * with a specific opcode.
	 * <p>
	 * The backing {@link ByteBuffer} is pre-allocated
	 * with 16 bytes by default.
	 *
	 * @param opcode
	 *      This {@link Packet}'s identifier;
	 *      Must range from {@code 0} to {@code 255}.
	 */
	public OutgoingPacket(int opcode) {
		this(16, opcode);
	}

	/**
	 * Instantiates a raw {@link Packet}
	 * with a specific opcode.
	 *
	 * @param size
	 *      The number of bytes to preallocate
	 *      the backing {@link ByteBuffer} with.
	 * @param opcode
	 *      This {@link Packet}'s identifier;
	 *      Must range from {@code 0} to {@code 255}.
	 */
	public OutgoingPacket(int size, int opcode) {
		payload = ByteBuffer.allocate(size).put((byte) opcode);
	}

	/**
	 * A method that allows each {@link Packet}
	 * implementation to determine how its data
	 * should be written to form its payload.
	 *
	 * @param payload
	 *      A {@link ByteBuffer} that will hold the
	 *      data belonging to this {@link Packet}.
	 */
	public abstract void write(ByteBuffer payload);

	/**
	 * Writes a single {@code byte} to this
	 * {@link Packet}'s payload.
	 *
	 * @param b
	 *      An {@code int} for ease-of-use,
	 *      but internally down-casted to a
	 *      {@code byte}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putByte(int b) {
		payload.put((byte) b);
		return this;
	}

	/**
	 * Writes a variable amount of
	 * {@code byte}s to this {@link Packet}'s
	 * payload.
	 *
	 * @param src
	 *      An {@code int} array for ease-of-use,
	 *      but each element is internally down-casted
	 *      to a {@code byte}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putBytes(int... src) {
		for (int b : src) {
			payload.put((byte) b);
		}

		return this;
	}

	/**
	 * Writes a single {@code char} to this
	 * {@link Packet}'s payload.
	 *
	 * @param c
	 *      A {@code char}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putChar(char c) {
		payload.putChar(c);
		return this;
	}

	/**
	 * Writes a single {@code double} to this
	 * {@link Packet}'s payload.
	 *
	 * @param d
	 *      A {@code double}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putDouble(double d) {
		payload.putDouble(d);
		return this;
	}


	/**
	 * Writes a single {@code float} to this
	 * {@link Packet}'s payload.
	 *
	 * @param f
	 *      A {@code float}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putFloat(float f) {
		payload.putFloat(f);
		return this;
	}

	/**
	 * Writes a single {@code int} to this
	 * {@link Packet}'s payload.
	 *
	 * @param i
	 *      A {@code int}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putInt(int i) {
		payload.putInt(i);
		return this;
	}

	/**
	 * Writes a single {@code long} to this
	 * {@link Packet}'s payload.
	 *
	 * @param l
	 *      A {@code long}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putLong(long l) {
		payload.putLong(l);
		return this;
	}

	/**
	 * Writes a single {@code short} to this
	 * {@link Packet}'s payload.
	 *
	 * @param s
	 *      A {@code short}.
	 * @return
	 *      The {@link Packet} to allow for
	 *      chained writes.
	 */
	public OutgoingPacket putShort(short s) {
		payload.putShort(s);
		return this;
	}

	/**
	 * Gets the payload of this {@link Packet}.
	 *
	 * @return
	 *      A {@link ByteBuffer} that holds the
	 *      data.
	 */
	protected ByteBuffer getPayload() {
		return payload;
	}

}
