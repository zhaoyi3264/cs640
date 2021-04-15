import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

// active
public class Sender extends TCPSocket {

    private LinkedBlockingQueue<byte[]> buffer;
    private Timeout timeout;

    public Sender(int port, int mtu, int sws, String file, InetAddress remoteAddress, int remotePort) {
        super(port, mtu, sws, file);
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.buffer = new LinkedBlockingQueue<>(sws);
        this.timeout = new Timeout(5_000_000_000L);

        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.socket.connect​(this.remoteAddress, this.remotePort);
        System.out.println("Sender operating on port " + this.port);
        // System.out.println("Sender sending to port " + this.remotePort);
    }

    @Override
    public void connect() {
        // snd SYN
        this.sendSyn();
        // rcv SYN-ACK
        TCPPacket tcp = this.receiveSynAck();
        this.timeout.update(tcp.seq, tcp.timestamp);
        // snd ACK
        this.sendAck(-1);
        System.out.println("Connection established");
    }

    private void producer() {
        try {
            for (int i = 0; i < 100; i++) {
                byte[] data = new byte[this.mtu];
                Arrays.fill(data, (byte)i);
                this.buffer.put(data);
                // skip package 2
                if (i == 1) {
                    this.seq += (this.mtu);
                    continue;
                }
                this.send(-1, false, false, false, data);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void consumer() {
        int lastAck = -1;
        int dup = 0;
        // int retransmit = 0;
        try {
            while (true) {
                TCPPacket tcp = this.receiveAck();
                this.timeout.update(tcp.seq, tcp.timestamp);
                if (tcp.ack == lastAck) {
                    dup += 1;
                    if (dup == 3) {
                        this.retransmit();
                        dup = 0;
                    }
                } else {
                    dup = 0;
                    this.buffer.take();
                }
                lastAck = tcp.ack;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void retransmit() {
        for (byte[] data : this.buffer) {
            this.seq -= data.length;
        }
        for (byte[] data : this.buffer) {
            this.send(-1, false, false, false, data);
        }
    }

    @Override
    public void run() {
        new Thread(() -> producer()).start();
        new Thread(() -> consumer()).start();
    }

    @Override
    protected void disconnect() {
        // snd FIN
        this.sendFin();
        // rcv ACK-FIN
        this.receiveAckFin();
        // snd ACK
        this.sendAck(-1);
        System.out.println("Connection closed");
    }
}