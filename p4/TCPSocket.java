import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class TCPSocket {

    protected static final byte[] EMPTY_DATA = {};

    protected int port;
    protected int mtu;
    protected int sws;
    protected String file;
    protected InetAddress remoteAddress;
    protected int remotePort;

    protected DatagramSocket socket;
    protected int seq;
    protected int ack;

    protected long start;

    public TCPSocket(int port, int mtu, int sws, String file) {
        this.port = port;
        this.mtu = mtu;
        this.sws = sws;
        this.file = file;

        this.seq = 0;
        this.ack = 0;
        this.start = System.nanoTime();
    }

    protected void send(int flags, byte[] data) throws IOException {
		DatagramPacket p = TCP.createPacket(this.remoteAddress, this.remotePort, this.seq, this.ack, flags, data);
        this.socket.send(p);
        double elapsed = (System.nanoTime() - this.start) / 1E9;
        char syn = ((flags & 4) == 4) ? 'S' : '-';
        char ack = ((flags & 2) == 2) ? 'A' : '-';
        char fin = ((flags & 1) == 1) ? 'F' : '-';
        char d = (data.length != 0) ? 'D' : '-';
        System.out.println(String.format("snd %.3f %c %c %c %c %d %d %d", elapsed, syn, ack, fin, d, this.seq, data.length, this.ack));
        this.seq += data.length;
    }

    protected TCP receive() throws IOException {
        byte[] buf = new byte[1024];
		DatagramPacket p = new DatagramPacket(buf, buf.length);
        socket.receive(p);
        // System.out.println("Receive p from remote port " + p.getPort());
        TCP tcp = TCP.asTCP(p);
        this.ack += tcp.length;
        double elapsed = (System.nanoTime() - this.start) / 1E9;
        char syn = tcp.SYN ? 'S' : '-';
        char ack = tcp.ACK ? 'A' : '-';
        char fin = tcp.FIN ? 'F' : '-';
        char d = (tcp.length != 0) ? 'D' : '-';
        System.out.println(String.format("rcv %.3f %c %c %c %c %d %d %d", elapsed, syn, ack, fin, d, tcp.seq, tcp.length, tcp.ack));
        return tcp;
    }

    protected void sendAck() throws IOException {
        this.send(TCP.calculateFlags(0, 1, 0), TCPSocket.EMPTY_DATA);
    }

    protected TCP receiveAck() throws IOException {
        TCP tcp = this.receive();
        if (tcp.SYN || !tcp.ACK || tcp.FIN) {
            throw new IllegalStateException("Did not receive ACK");
        }
        return tcp;
    }
}