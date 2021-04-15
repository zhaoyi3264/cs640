import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

// active
public class Sender extends TCPSocket {

    private volatile boolean prodStopped;

    private LinkedBlockingQueue<BufferEntry> buffer;
    private Timeout timeout;

    public Sender(int port, int mtu, int sws, String file, InetAddress remoteAddress, int remotePort) {
        super(port, mtu, sws, file);
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.prodStopped = false;
        this.buffer = new LinkedBlockingQueue<>(sws);
        this.timeout = new Timeout(5_000_000_000L);

        try {
            this.socket = new DatagramSocket(port);
        } catch(SocketException e) {
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
            for (int i = 0; i < 10; i++) {
                byte[] data = new byte[this.mtu];
                Arrays.fill(data, (byte)i);
                this.buffer.put(new BufferEntry(this.seq, data));
                // skip package(s)
                if (false) {
                    this.seq += (this.mtu);
                    continue;
                }
                this.send(-1, false, false, false, data);
            }
            this.prodStopped = true;
            System.out.println("Producer stop");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void consumer() {
        int lastAck = 1;
        int dup = 0;
        // int retransmit = 0;
        TCPPacket tcp;
        try {
            while (true) {
                tcp = this.receiveAck();
                this.timeout.update(tcp.seq, tcp.timestamp);
                if (tcp.ack == lastAck) {
                    dup += 1;
                    System.out.println("Duplicate " + dup);
                    if (dup == 3) {
                        this.retransmit();
                        dup = 0;
                    }
                } else {
                    dup = 0;
                    BufferEntry be = this.buffer.take();
                    System.out.println("Remove " + be.seq);
                    while ((be.seq + be.data.length) < tcp.ack - 1 && this.buffer.size() > 0) {
                        be = this.buffer.take();
                        System.out.println("Remove " + be.seq);
                    }
                }
                lastAck = tcp.ack;
                if (this.prodStopped && this.buffer.size() == 0) {
                    System.out.println("Consumer stop");
                    break;
                }
            }
            // disconnect
            this.sendFin();
            System.out.println("FIN sent");
            this.receiveAckFin();
            System.out.println("ACK-FIN received");
            this.sendAck(-1);
            System.out.println("ACK sent");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void retransmit() {
        for (BufferEntry be : this.buffer) {
            this.seq -= be.data.length;
        }
        for (BufferEntry be : this.buffer) {
            this.send(-1, false, false, false, be.data);
        }
    }

    @Override
    public void run() {
        new Thread(() -> producer()).start();
        new Thread(() -> consumer()).start();
    }
}

class BufferEntry {
    int seq;
    byte[] data;

    public BufferEntry(int seq, byte[] data) {
        this.seq = seq;
        this.data = data;
    }
}