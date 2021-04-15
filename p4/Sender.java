import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

// active
public class Sender extends TCPSocket {

    private volatile boolean prodStopped;

    static private Timer timer;
    private LinkedBlockingQueue<BufferEntry> buffer;
    private Timeout timeout;

    public Sender(int port, int mtu, int sws, String file, InetAddress remoteAddress, int remotePort) {
        super(port, mtu, sws, file);
        this.state = TCPState.CLOSED;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.prodStopped = false;
        this.timer = new Timer(true);
        this.buffer = new LinkedBlockingQueue<>(sws);
        this.timeout = new Timeout(5_000_000_000L);

        try {
            this.socket = new DatagramSocket(port);
            this.socket.setSoTimeout(5_000);
        } catch(SocketException e) {
            e.printStackTrace();
        }
        this.socket.connect​(this.remoteAddress, this.remotePort);
        System.out.println("Sender operating on port " + this.port);
        // System.out.println("Sender sending to port " + this.remotePort);
    }

    @Override
    public void connect() {
        int retrans = 0;
        while(true) {
            switch(this.state) {
                case CLOSED:
                    if (retrans > TCPSocket.MAX_RETRANSMIT) {
                        System.err.println("Max retransmit time exceeded");
                        System.exit(1);
                    }
                    this.sendSyn();
                    retrans += 1;
                    this.state = TCPState.SYN_SENT;
                    break;
                case SYN_SENT:
                    // receive syn-ack
                    TCPPacket tcp = this.receiveSynAck();
                    if (tcp != null) {
                        this.sendAck(-1);
                        this.state = TCPState.ESTABLISHED;
                    } else { // timeout
                        this.seq -= 1;
                        this.ack -= 1;
                        this.state = TCPState.CLOSED;
                    }
                    break;
                case ESTABLISHED:
                    System.out.println("Connection established");
                    return;
            }
        }
    }

    private TimerTask schedRetrans(int seq, boolean SYN, boolean ACK, boolean FIN, byte[] data) {
        TimerTask retrans = new TimerTask() {
            int retransTime = 0;
            @Override
            public void run() {
                if (retransTime < 3) {
                    resend(seq, SYN, ACK, FIN, data);
                    retransTime += 1;
                } else {
                    this.cancel();
                    System.out.println("Retransmission failed");
                }
            }
        };
        long delay = this.timeout.getTo() / 1_000_000;
        this.timer.scheduleAtFixedRate(retrans, delay, delay);
        return retrans;
    }

    protected void resend(int seq, boolean SYN, boolean ACK, boolean FIN,  byte[] data) {
        int old = this.seq;
        this.send(-1, SYN, ACK, FIN, data);
        this.seq = old;
    }

    private void producer() {
        try {
            for (int i = 0; i < 10; i++) {
                byte[] data = new byte[this.mtu];
                Arrays.fill(data, (byte)i);
                this.buffer.put(new BufferEntry(this.seq, data, null));
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
    TimerTask retrans;

    public BufferEntry(int seq, byte[] data, TimerTask retrans) {
        this.seq = seq;
        this.data = data;
        this.retrans = retrans;
    }
}