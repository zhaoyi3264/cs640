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
                tcp = this.receive();
                if (tcp.seq == (this.ack - tcp.data.length)) {
                    this.buffer.put(tcp);
                } else {
                    this.ack -= tcp.data.length;
                    System.out.println("Incorrect seq");
                }
                if (tcp.FIN) {
                    System.out.println("Producer stop");
                    break;
                }
                if (tcp.seq == 9 || tcp.seq == 25 || tcp.seq == 17) {
                    continue;
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
                if (tcp.FIN) {
                    System.out.println("FIN received");
                    System.out.println("Consumer stop");
                    break;
                }
                System.out.println("Write data " + Arrays.toString(tcp.data));
            }
            // disconnect
            this.sendAckFin();
            System.out.println("ACK-FIN sent");
            this.receiveAck();
            System.out.println("ACK received");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        new Thread(() -> producer()).start();
        new Thread(() -> consumer()).start();
    }
}