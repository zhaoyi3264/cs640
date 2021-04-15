import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

// passive
public class Receiver extends TCPSocket {

    private LinkedBlockingQueue<TCPPacket> buffer;

    public Receiver(int port, int mtu, int sws, String file) {
        super(port, mtu, sws, file);
        this.buffer = new LinkedBlockingQueue<>(sws);
        try {
            this.socket = new DatagramSocket(this.port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("Receiver listening on port " + this.port);
    }

    public void connect() {
        // rcv SYN
        TCPPacket tcp = this.receiveSyn();
        // snd SYN-ACK
        this.sendSynAck(tcp.timestamp);
        // rcv ACK
        this.receiveAck();
        System.out.println("Connection established");
    }

    private void producer() {
        try{
            while (true) {
                TCPPacket tcp = this.receive();
                if (tcp.seq == (this.ack - tcp.data.length)) {
                    this.buffer.put(tcp);
                } else {
                    this.ack -= tcp.data.length;
                }
                this.sendAck(tcp.timestamp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void consumer() {
        try {
            while (true) {
                TCPPacket tcp = this.buffer.take();
                System.out.println("Write data " + Arrays.toString(tcp.data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        new Thread(() -> producer()).start();
        new Thread(() -> consumer()).start();
    }

    public void disconnect() {
        // rcv FIN
        this.receiveFin();
        // snd ACK-FIN
        this.sendAckFin();
        // rcv ACK
        this.receiveAck();
        System.out.println("Connection closed");
    }

    private TCPPacket receiveSyn() {
        this.ack += 1;
        TCPPacket tcp = this.receive();
        if (!tcp.SYN || tcp.ACK || tcp.FIN) {
            throw new IllegalStateException("Did not receive SYN");
        }
        this.remoteAddress = tcp.remoteAddress;
        this.remotePort = tcp.remotePort;
        return tcp;
    }

    private void sendSynAck(long timestamp) {
        this.send(timestamp, true, true, false, TCPSocket.EMPTY_DATA);
        this.seq += 1;
    }

    private TCPPacket receiveFin() {
        this.ack += 1;
        return null;
    }

    private void sendAckFin() {
        this.send(-1, false, true, true, TCPSocket.EMPTY_DATA);
        this.seq += 1;
    }
}