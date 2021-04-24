import java.io.FileInputStream;
import java.io.IOException;
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

    private int retrans;

    private Timer timer;
    private LinkedBlockingQueue<BufferEntry> buffer;
    private Timeout timeout;
    private FileInputStream input;

    public Sender(int port, int mtu, int sws, String file, InetAddress remoteAddress, int remotePort) {
        super(port, mtu, sws, file);
        this.retrans = 0;
        this.state = TCPState.CLOSED;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;

        this.timer = new Timer(true);
        this.buffer = new LinkedBlockingQueue<>(sws);
        this.timeout = new Timeout(TCPSocket.DEFAULT_TIMEOUT * 1_000_000L);

        try {
            this.socket = new DatagramSocket(port);
            this.socket.setSoTimeout(TCPSocket.DEFAULT_TIMEOUT);
            this.input = new FileInputStream(this.file);
        } catch(SocketException e) {
            e.printStackTrace();
        } catch(IOException e) {
            System.err.println("Cannot read file " + this.file);
            System.exit(1);
        }
        this.socket.connect​(this.remoteAddress, this.remotePort);
        System.out.println("Sender operating on port " + this.port);
        // System.out.println("Sender sending to port " + this.remotePort);
    }

    private void checkRetrans() {
        if(this.retrans > TCPSocket.MAX_RETRANSMIT) {
            System.err.println("Max retransmit time exceeded");
            System.exit(1);
        }
        this.retrans += 1;
    }

    @Override
    public void connect() {
        while(true) {
            switch(this.state) {
                case CLOSED:
                    checkRetrans();
                    this.sendSyn();
                    this.state = TCPState.SYN_SENT;
                    break;
                case SYN_SENT:
                    // receive syn-ack
                    TCPPacket tcp = this.receiveSynAck();
                    if(tcp != null) {
                        this.timeout.update(tcp.seq, tcp.timestamp);
                        this.sendAck(-1);
                        this.state = TCPState.ESTABLISHED;
                    } else { // timeout
                        this.seq -= 1;
                        this.ack -= 1;
                        this.state = TCPState.CLOSED;
                    }
                    break;
                case ESTABLISHED:
                    this.retrans = 0;
                    System.out.println("Connection established");
                    return;
            }
        }
    }

    private TimerTask createTask(int seq, byte[] data) {
        TimerTask task = new TimerTask() {
            int retransTime = 0;
            @Override
            public void run() {
                if(retransTime > TCPSocket.MAX_RETRANSMIT) {
                    this.cancel();
                    System.err.println("Retransmit failed");
                    System.exit(1);
                } else {
                    System.err.println("Retransmit " + seq);
                    resend(seq, data);
                    retransTime += 1;
                }
            }
        };
        return task;
    }

    protected void resend(int seq, byte[] data) {
        synchronized(this) {
            int old = this.seq;
            this.seq = seq;
            this.send(-1, false, false, false, data);
            this.seq = old;
        }
    }

    private byte[] readData() {
        byte[] data = new byte[this.mtu];
        int len = -1;
        try {
            len = this.input.read(data);
        } catch(IOException e) {
            System.err.println("Cannot read file " + this.file);
            System.exit(1);
        }
        if (len != -1 && len < this.mtu) {
            data = Arrays.copyOf(data, len);
        }
        return len != -1 ? data : null;
    }

    private void producer() {
        while (true) {
            switch(this.state) {
                case ESTABLISHED:
                    // System.out.println("ESTABLISHED");
                    byte[] data = this.readData();
                    if(data == null) {
                        if(this.buffer.size() == 0) {
                            checkRetrans();
                            this.sendFin();
                            this.state = TCPState.FIN_WAIT;
                        }
                    } else {
                        TimerTask task = this.createTask(this.seq - data.length, data);
                        BufferEntry be = new BufferEntry(this.seq, data, task);
                        try {
                            this.buffer.put(be);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                        // if(this.seq == 9) {
                        //     this.seq += this.mtu;
                        //     continue;
                        // }
                        this.send(-1, false, false, false, data);
                        long delay = this.timeout.getTo() / 1_000_000;
                        try {
                            this.timer.scheduleAtFixedRate(task, delay, delay);
                        } catch(Exception e) {
                            
                        }
                    }
                    break;
                case FIN_WAIT:
                    // System.out.println("FIN_WAIT");
                    checkRetrans();
                    TCPPacket tcp = this.receiveAckFin();
                    if(tcp != null) {
                        this.sendAck(-1);
                        this.state = TCPState.TIME_WAIT;
                    } else {
                        this.seq -= 1;
                        this.ack -= 1;
                        this.state = TCPState.ESTABLISHED;
                    }
                    break;
                case TIME_WAIT:
                    try {
                        this.input.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Connection closed");
                    return;
                case CLOSED:
                    break;
            }
        }
    }

    private void consumer() {
        int lastAck = 1;
        int dup = 0;
        while (true) {
            switch(this.state) {
                case ESTABLISHED:
                        if(this.buffer.isEmpty()) {
                            continue;
                        }
                        TCPPacket tcp = this.receiveAck();
                        if(tcp == null) {
                            continue;
                        }
                        this.timeout.update(tcp.seq, tcp.timestamp);
                        if(tcp.ack == lastAck) {
                            dup += 1;
                            System.out.println("Duplicate " + dup);
                            if(dup == 3) {
                                this.retransmit();
                                dup = 0;
                            }
                        } else {
                            dup = 0;
                            BufferEntry be = null;
                            try {
                                do {
                                    be = this.buffer.take();
                                    be.task.cancel();
                                    System.out.println("Remove " + be.seq);
                                } while ((be.seq + be.data.length) < tcp.ack - 1 && this.buffer.size() > 0);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                        lastAck = tcp.ack;
                    break;
                case FIN_WAIT:
                case TIME_WAIT:
                case CLOSED:
                    return;
            }
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
        Thread p = new Thread(() -> producer());
        Thread c = new Thread(() -> consumer());
        p.start();
        c.start();
    }
}

class BufferEntry {
    int seq;
    byte[] data;
    TimerTask task;

    public BufferEntry(int seq, byte[] data, TimerTask task) {
        this.seq = seq;
        this.data = data;
        this.task = task;
    }
}