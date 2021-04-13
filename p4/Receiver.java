import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;;
import java.nio.ByteBuffer;
import java.util.Arrays;


// passive
public class Receiver extends TCPUser {

    public Receiver(int port, int mtu, int sws, String file) {
        super(port, mtu, sws, file);
        try {
            this.socket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("Receiver listening on port " + this.port);
    }

    public void connect() throws IOException {
        byte[] data = {};
        // rcv SYN
        this.receiveSyn();
        // snd SYN-ACK
        this.sendSynAck();
        // rcv ACK
        this.receiveAck();
        System.out.println("Connection established");
    }

    public void run() throws IOException {
        // rcv data 
        TCP tcp = this.receive();
        // snd ack
        this.sendAck();

        this.disconnect();
    }

    public void disconnect() throws IOException {
        // rcv FIN
        this.receiveFin();
        // snd ACK-FIN
        this.sendAckFin();
        // rcv ACK
        this.receiveAck();
        System.out.println("Connection closed");
    }

    private void receiveSyn() throws IOException {
        this.ack += 1;
        TCP tcp = this.receive();
        if (!tcp.SYN || tcp.ACK || tcp.FIN) {
            throw new IllegalStateException("Did not receive SYN");
        }
        this.remoteAddress = tcp.remoteAddress;
        this.remotePort = tcp.remotePort;
    }

    private void sendSynAck() throws IOException {
        this.send(TCP.calculateFlags(1, 1, 0), TCPUser.EMPTY_DATA);
        this.seq += 1;
    }

    private TCP receiveFin() throws IOException {
        this.ack += 1;
        TCP tcp = this.receive();
        if (tcp.SYN || tcp.ACK || !tcp.FIN) {
            throw new IllegalStateException("Did not receive FIN");
        }
        return tcp;
    }

    private void sendAckFin() throws IOException {
        this.send(TCP.calculateFlags(0, 1, 1), TCPUser.EMPTY_DATA);
        this.seq += 1;
    }
}