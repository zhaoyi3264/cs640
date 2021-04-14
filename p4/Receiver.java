import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;


// passive
public class Receiver extends TCPSocket {

    private LinkedBlockingQueue<TCP> buffer;

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

    private void producer() {
        try{
            while (true) {
                TCP tcp = this.receive();
                if (tcp.seq == (this.ack - tcp.length)) {
                    this.buffer.put(tcp);
                } else {
                    this.ack -= tcp.length;
                }
                this.sendAck();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void consumer() {
        try {
            while (true) {
                TCP tcp = this.buffer.take();
                System.out.println("Write data " + Arrays.toString(tcp.data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        new Thread(() -> producer()).start();
        new Thread(() -> consumer()).start();
        // while (true) {
        //     // rcv data 
        //     TCP tcp = this.receive();
        //     if (tcp.SYN) {
        //         throw new IllegalStateException("Did not expect SYN");
        //     }
        //     if (tcp.ACK && tcp.FIN) {
        //         throw new IllegalStateException("Did not expect ACK-FIN");
        //     }
        //     if (tcp.ACK) {
        //         // snd ack
        //         this.sendAck();
        //     }
        //     if (tcp.FIN) {
        //         this.disconnect();
        //         break;
        //     }
        // }
        // this.disconnect();
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
        this.send(TCP.calculateFlags(1, 1, 0), TCPSocket.EMPTY_DATA);
        this.seq += 1;
    }

    private TCP receiveFin() throws IOException {
        this.ack += 1;
        return null;
    }

    private void sendAckFin() throws IOException {
        this.send(TCP.calculateFlags(0, 1, 1), TCPSocket.EMPTY_DATA);
        this.seq += 1;
    }
}