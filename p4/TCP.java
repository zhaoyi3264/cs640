import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCP {
	
	public static final int HLEN = 24;
	
	public int seq;
	public int ack;
	public long timestamp;
	public int length;
	public boolean SYN;
	public boolean ACK;
	public boolean FIN;
	public int checksum;
	public byte[] data;

	public InetAddress remoteAddress;
	public int remotePort;

	public TCP (int seq, int ack, long timestamp, int length, boolean SYN,
				boolean ACK, boolean FIN, int checksum, byte[] data) {
		this.seq = seq;
		this.ack = ack;
		this.timestamp = timestamp;
		this.length = length;
		this.SYN = SYN;
		this.ACK = ACK;
		this.FIN = FIN;
		this.checksum = checksum;
		this.data = data;
	}

	public static int calculateFlags(int SYN, int ACK, int FIN) {
		return 4 * SYN + 2 * ACK + FIN;
	}

	public static DatagramPacket createPacket(InetAddress address, int port, int seq, int ack, int flags, byte[] data) {
		int length = data.length;
		ByteBuffer bb = ByteBuffer.allocate(TCP.HLEN + length);
		// seq
		bb.putInt(seq);
		// ack
		bb.putInt(ack);
		// timestamp
		bb.putLong(System.nanoTime());
		// length and flags
		bb.putInt((length << 3) + flags);
		// checksum
		bb.putInt(0);
		// data
		bb.put(data);
		byte[] buf = bb.array();
		// TODO: compute checksum
		return new DatagramPacket(buf, 0, TCP.HLEN + length, address, port);
	}
	
	public static TCP asTCP(DatagramPacket p) {
		int length = p.getLength() - TCP.HLEN;
		ByteBuffer bb = ByteBuffer.allocate(TCP.HLEN + length).wrap(p.getData());
		// seq
		int seq = bb.getInt();
		// ack
		int ack = bb.getInt();
		// timestamp
		long timestamp = bb.getLong();
		// length and flags
		int l = bb.getInt();
		if ((l >>> 3) != length) {
			throw new IllegalStateException(String.format("Package length is not set correctly. (%d, %d)", (l >>> 3), length));
		}
		boolean SYN = (l & 4) == 4;
		boolean ACK = (l & 2) == 2;
		boolean FIN = (l & 1) == 1;
		// checksum
		int checksum = bb.getInt();
		// data
		byte[] data = new byte[length];
		bb.get​(data, 0, length);
		TCP tcp = new TCP(seq, ack, timestamp, length, SYN, ACK, FIN, checksum, data);
		tcp.remoteAddress = p.getAddress();
		tcp.remotePort = p.getPort();
		return tcp;
	}
	
	public String toString() {
		return String.format("seq=%d, ack=%d, timestamp=%d, length=%d, checksum=%d, ",
				seq, ack, timestamp, length, checksum)
			+ String.format("SYN=%b, ACK=%b, FIN=%b, Data=", SYN, ACK, FIN)
			+ Arrays.toString(data);
	}
}
