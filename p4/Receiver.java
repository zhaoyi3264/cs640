import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

// passive
public class Receiver extends TCPSocket {

    private volatile boolean finReceived;

    private LinkedBlockingQueue<TCPPacket> buffer;

    public Receiver(int port, int mtu, int sws, String file) {
        super(port, mtu, sws, file);
        this.finReceived = false;
        this.buffer = new LinkedBlockingQueue<>(sws);
        try {
            this.socket = new DatagramSocket(this.port);
        } catch(SocketException e) {
            e.printStackTrace();
        }
        System.out.println("Receiver listening on port " + this.port);
    }

    @Override
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
        TCPPacket tcp;
        try {
            while(true) {
                System.out.println("here");
                if (this.finReceived) {
                    break;
                } else {
                    tcp = this.receive();
                }
                if (tcp.seq == (this.ack - tcp.data.length)) {
                    this.buffer.put(tcp);
                } else {
                    this.ack -= tcp.data.length;
                    System.out.println("Incorrect seq");
                }
                if (!tcp.SYN && !tcp.ACK && !tcp.FIN) {
                    this.sendAck(tcp.timestamp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void consumer() {
        try {
            while (!this.finReceived) {
                TCPPacket tcp = this.buffer.take();
                if (!this.finReceived) {
                    this.finReceived = tcp.FIN;
                }
                System.out.println("buffer size " + this.buffer.size());
                System.out.println(this.finReceived);
                if (tcp.SYN || tcp.ACK || tcp.FIN) {
                    continue;
                }
                System.out.println("Write data " + Arrays.toString(tcp.data));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        new Thread(() -> producer()).start();
        new Thread(() -> consumer()).start();
        new Thread(() -> {
            while(!this.finReceived || this.buffer.size() > 0) {
            }
            this.sendAckFin();
            System.out.println("ACK-FIN sent");
            this.receiveAck();
            System.out.println("ACK received");
        }).start();
    }

    @Override
    protected void disconnect() {
        // rcv FIN
        this.receiveFin();
        // snd ACK-FIN
        this.sendAckFin();
        // rcv ACK
        this.receiveAck();
        System.out.println("Connection closed");
    }
}