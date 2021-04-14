import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

// active
public class Sender extends TCPSocket {

    private LinkedBlockingQueue<byte[]> buffer;

    public Sender(int port, int mtu, int sws, String file, InetAddress remoteAddress, int remotePort) {
        super(port, mtu, sws, file);
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.buffer = new LinkedBlockingQueue<>(sws);

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

    private void producer() {
        int flags = TCP.calculateFlags(0, 0, 0);
        try {
            for (int i = 0; i < 7; i++) {
                byte[] data = new byte[this.mtu - i];
                Arrays.fill(data, (byte)i);
                this.buffer.put(data);
                // skip package 2
                if (i == 1) {
                    this.seq += (this.mtu - i);
                    continue;
                }
                this.send(flags, data);
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
                TCP tcp = this.receiveAck();
                if (tcp.ack == lastAck) {
                    dup += 1;
                    if (dup == 3) {
                        this.retransmit();
                        dup = 0;
                    }
                } else {
                this.buffer.take();
                }
                lastAck = tcp.ack;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void retransmit() throws IOException {
        for (byte[] data : this.buffer) {
            this.seq -= data.length;
        }
        for (byte[] data : this.buffer) {
            this.send(0, data);
        }
    }

    public void run() throws IOException {
        new Thread(() -> producer()).start();
        new Thread(() -> consumer()).start();
        // byte[] data = new byte[56];
        // for (int i = 0; i < 2; i++) {
        //     // snd data
        //     this.send(TCP.calculateFlags(0, 1, 0), data);
        //     // rcv ack
        //     this.receiveAck();
        // }
        // this.disconnect();
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
        this.send(TCP.calculateFlags(1, 0, 0), TCPSocket.EMPTY_DATA);
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
        this.send(TCP.calculateFlags(0, 0, 1), TCPSocket.EMPTY_DATA);
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