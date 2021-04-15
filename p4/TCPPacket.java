import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCPPacket {
    
    public static final int HLEN = 24;

    public InetAddress remoteAddress;
    public int remotePort;
    
    public int seq;
    public int ack;
    public long timestamp;
    public boolean SYN;
    public boolean ACK;
    public boolean FIN;
    public int checksum;
    public byte[] data;

    public TCPPacket(InetAddress remoteAddress, int remotePort, int seq, int ack, long timestamp,
        boolean SYN, boolean ACK, boolean FIN, byte[] data) {
        
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;

        this.seq = seq;
        this.ack = ack;
        this.timestamp = timestamp;
        this.SYN = SYN;
        this.ACK = ACK;
        this.FIN = FIN;
        this.checksum = 0;
        this.data = data;
    }

    private int calculateFlags() {
        return 4 * (this.SYN ? 1 : 0) + 2 * (this.ACK ? 1 : 0) + (this.FIN ? 1 : 0);
    }

    public DatagramPacket toDatagramPacket() {
        int length = this.data.length;
        ByteBuffer bb = ByteBuffer.allocate(TCPPacket.HLEN + length);
        // seq
        bb.putInt(seq);
        // ack
        bb.putInt(ack);
        // timestamp
        if (timestamp == -1) {
            bb.putLong(System.nanoTime());
        } else {
            bb.putLong(timestamp);
        }
        // length and flags
        bb.putInt((length << 3) + this.calculateFlags());
        // checksum
        bb.putInt(0);
        // data
        bb.put(data);
        byte[] buf = bb.array();
        // TODO: compute checksum
        return new DatagramPacket(buf, 0, TCPPacket.HLEN + length, remoteAddress, remotePort);
    }
    
    public static TCPPacket fromDatagramPacket(DatagramPacket p) {
        int length = p.getLength() - TCPPacket.HLEN;
        ByteBuffer bb = ByteBuffer.allocate(TCPPacket.HLEN + length).wrap(p.getData());
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

        InetAddress remoteAddress = p.getAddress();
        int remotePort = p.getPort();

        TCPPacket tcp = new TCPPacket(remoteAddress, remotePort, seq, ack, timestamp, SYN, ACK, FIN, data);
        return tcp;
    }
    
    public String toString() {
        return String.format("seq=%d, ack=%d, timestamp=%d, length=%d, checksum=%d, ",
                seq, ack, timestamp, data.length, checksum)
            + String.format("SYN=%b, ACK=%b, FIN=%b, Data=", SYN, ACK, FIN)
            + Arrays.toString(data);
    }
}
