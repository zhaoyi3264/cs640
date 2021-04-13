import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;;
import java.nio.ByteBuffer;
import java.util.Arrays;

// active
public class Sender extends TCPUser {

    public Sender(int port, int mtu, int sws, String file, InetAddress remoteAddress, int remotePort) {
        super(port, mtu, sws, file);
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;

        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.socket.connect​(this.remoteAddress, this.remotePort);
        System.out.println("Sender operating on port " + this.port);
        // System.out.println("Sender sending to port " + this.remotePort);
    }

    public void connect() throws IOException {
        // snd SYN
        this.sendSyn();
        // rcv SYN-ACK
        this.receiveSynAck();
        // snd ACK
        this.sendAck();
        System.out.println("Connection established");
    }

    public void run() throws IOException {
        byte[] data = new byte[112];
        // snd data
        this.send(TCP.calculateFlags(0, 1, 0), data);
        // rcv ack
        this.receiveAck();

        this.disconnect();
    }

    private void disconnect() throws IOException {
        // snd FIN
        this.sendFin();
        // rcv ACK-FIN
        this.receiveAckFin();
        // snd ACK
        this.sendAck();
        System.out.println("Connection closed");
    }

    private void sendSyn() throws IOException {
        this.send(TCP.calculateFlags(1, 0, 0), TCPUser.EMPTY_DATA);
        this.seq += 1;
    }

    private void receiveSynAck() throws IOException {
        this.ack += 1;
        TCP tcp = this.receive();
        if (!tcp.SYN || !tcp.ACK || tcp.FIN) {
            throw new IllegalStateException("Did not receive SYN-ACK");
        }
    }

    private void sendFin() throws IOException {
        this.send(TCP.calculateFlags(0, 0, 1), TCPUser.EMPTY_DATA);
        this.seq += 1;
    }

    private TCP receiveAckFin() throws IOException {
        this.ack += 1;
        TCP tcp = this.receive();
        if (tcp.SYN || !tcp.ACK || !tcp.FIN) {
            throw new IllegalStateException("Did not receive ACK-FIN");
        }
        return tcp;
    }
}