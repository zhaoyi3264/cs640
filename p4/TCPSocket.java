import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class TCPSocket {

    public static final int MAX_RETRANSMIT = 16;
    public static final int DEFAULT_TIMEOUT = 5000;

    protected volatile TCPState state;

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

        if(mtu > 1430) {
            throw new IllegalArgumentException("MTU must be less than or equal to 1430");
        }

        this.port = port;
        this.mtu = mtu;
        this.sws = sws;
        this.file = file;

        this.seq = 0;
        this.ack = 0;
        this.start = System.nanoTime();
    }

    public abstract void connect();

    public abstract void run();

    protected void send(long timestamp, boolean SYN, boolean ACK, boolean FIN,  byte[] data) {
		DatagramPacket p = new TCPPacket(this.remoteAddress, this.remotePort, this.seq,
            this.ack, timestamp, SYN, ACK, FIN, data).toDatagramPacket();
        try {
            this.socket.send(p);
        } catch(IOException e){
            e.printStackTrace();
        }
        double elapsed = (System.nanoTime() - this.start) / 1E9;
        char syn = SYN ? 'S' : '-';
        char ack = ACK ? 'A' : '-';
        char fin = FIN ? 'F' : '-';
        char d = (data.length != 0) ? 'D' : '-';
        System.out.println(String.format("snd %.3f %c %c %c %c %d %d %d",
            elapsed, syn, ack, fin, d, this.seq, data.length, this.ack));
        // System.out.println(Arrays.toString(data));
        this.seq += data.length;
    }

    protected TCPPacket receive() {
        byte[] buf = new byte[TCPPacket.HLEN + this.mtu];
		DatagramPacket p = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(p);
        } catch(PortUnreachableException e) {
            System.out.println("Port unreachable");
            System.exit(1);
        } catch(SocketTimeoutException e) {
            System.out.println("Socket receive timeout");
            return null;
        } catch(IOException e) {
            e.printStackTrace();
        }
        // System.out.println("Receive p from remote port " + p.getPort());
        TCPPacket tcp = TCPPacket.fromDatagramPacket(p);
        this.ack += tcp.data.length;
        double elapsed = (System.nanoTime() - this.start) / 1E9;
        char syn = tcp.SYN ? 'S' : '-';
        char ack = tcp.ACK ? 'A' : '-';
        char fin = tcp.FIN ? 'F' : '-';
        char d = (tcp.data.length != 0) ? 'D' : '-';
        System.out.println(String.format("rcv %.3f %c %c %c %c %d %d %d",
            elapsed, syn, ack, fin, d, tcp.seq, tcp.data.length, tcp.ack));
        // System.out.println(Arrays.toString(tcp.data));
        return tcp;
    }

    // for both
    protected void sendAck(long timestamp) {
        this.send(timestamp, false, true, false, TCPPacket.EMPTY_DATA);
    }

    protected TCPPacket receiveAck() {
        TCPPacket tcp = this.receive();
        if(tcp == null || tcp.SYN || !tcp.ACK || tcp.FIN) {
            System.err.println("Did not receive ACK");
        }
        return tcp;
    }

    // for sender
    protected void sendSyn() {
        this.send(-1, true, false, false, TCPPacket.EMPTY_DATA);
        this.seq += 1;
    }

    protected TCPPacket receiveSynAck() {
        this.ack += 1;
        TCPPacket tcp = this.receive();
        if(tcp == null || !tcp.SYN || !tcp.ACK || tcp.FIN) {
            System.err.println("Did not receive SYN-ACK");
        }
        return tcp;
    }

    protected void sendFin() {
        this.send(-1, false, false, true, TCPPacket.EMPTY_DATA);
        this.seq += 1;
    }

    protected TCPPacket receiveAckFin() {
        this.ack += 1;
        TCPPacket tcp = this.receive();
        if(tcp == null || tcp.SYN || !tcp.ACK || !tcp.FIN) {
            System.err.println("Did not receive ACK-FIN");
        }
        return tcp;
    }

    // for receiver
    protected TCPPacket receiveSyn() {
        this.ack += 1;
        TCPPacket tcp = this.receive();
        if(tcp == null || !tcp.SYN || tcp.ACK || tcp.FIN) {
            System.err.println("Did not receive SYN");
        } else {
            this.remoteAddress = tcp.remoteAddress;
            this.remotePort = tcp.remotePort;
        }
        return tcp;
    }

    protected void sendSynAck(long timestamp) {
        this.send(timestamp, true, true, false, TCPPacket.EMPTY_DATA);
        this.seq += 1;
    }

    protected TCPPacket receiveFin() {
        this.ack += 1;
        return null;
    }

    protected void sendAckFin() {
        this.ack += 1;
        this.send(-1, false, true, true, TCPPacket.EMPTY_DATA);
        this.seq += 1;
    }
}