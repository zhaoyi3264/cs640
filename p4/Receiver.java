import java.io.FileOutputStream;
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
    private FileOutputStream output;

    public Receiver(int port, int mtu, int sws, String file) {
        super(port, mtu, sws, file);
        this.state = TCPState.LISTEN;

        this.buffer = new LinkedBlockingQueue<>(sws);
        
        try {
            this.socket = new DatagramSocket(this.port);
            this.socket.setSoTimeout(TCPSocket.DEFAULT_TIMEOUT);
            this.output = new FileOutputStream(this.file);
        } catch(SocketException e) {
            e.printStackTrace();
        } catch(IOException e){
            System.err.println("Cannot write file " + this.file);
            System.exit(1);
        }
        System.out.println("Receiver listening on port " + this.port);
    }

    @Override
    public void connect() {
        while(true) {
            switch(this.state) {
                case LISTEN:
                    TCPPacket tcp = this.receiveSyn();
                    try {
                        Thread.sleep(1000);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    if(tcp != null) {
                        this.sendSynAck(tcp.timestamp);
                        this.state = TCPState.SYN_RECEIVED;
                    }
                    break;
                case SYN_RECEIVED:
                    this.receiveAck();
                    this.state = TCPState.ESTABLISHED;
                    break;
                case ESTABLISHED:
                    System.out.println("Connection established");
                    return;
            }
        }
    }

    private void producer() {
        while(true) {
            switch(this.state) {
                case ESTABLISHED:
                    TCPPacket tcp = this.receive();
                    if (tcp == null) {
                        continue;
                    }
                    if(tcp.seq == (this.ack - tcp.data.length)) {
                        try {
                            this.buffer.put(tcp);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        this.ack -= tcp.data.length;
                        System.out.println("Incorrect seq " + tcp.seq);
                    }
                    if(tcp.FIN) {
                        return;
                    }
                    // try {
                    //     Thread.sleep(1000);
                    // } catch(Exception e) {
                    //     e.printStackTrace();
                    // }
                    this.sendAck(tcp.timestamp);
                    break;
                case CLOSE_WAIT:
                case LAST_ACK:
                    break;
                case CLOSED:
                    return;
            }
        }
    }

    private void consumer() {
        int retrans = 0;
        while(true) {
            switch(this.state) {
                case ESTABLISHED:
                    // System.out.println("ESTABLISHED");
                    try {
                        TCPPacket tcp = this.buffer.take();
                        if(tcp.FIN) {
                            this.state = TCPState.CLOSE_WAIT;
                            this.output.close();
                        } else {
                            // System.out.println("Write data " + Arrays.toString(tcp.data));
                            this.output.write(tcp.data);
                        }
                    } catch(IOException e){
                        System.out.println("Cannot write file " + this.file);
                        System.exit(1);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case CLOSE_WAIT:
                    // System.out.println("CLOSE_WAIT");
                    if(retrans > TCPSocket.MAX_RETRANSMIT) {
                        System.err.println("Max retransmit time exceeded");
                        System.exit(1);
                    }
                    retrans += 1;
                    this.sendAckFin();
                    this.state = TCPState.LAST_ACK;
                    break;
                case LAST_ACK:
                    // System.out.println("LAST_ACK");
                    if(retrans > TCPSocket.MAX_RETRANSMIT) {
                        System.err.println("Max retransmit time exceeded");
                        System.exit(1);
                    }
                    retrans += 1;
                    TCPPacket tcp = this.receiveAck();
                    if(tcp != null && tcp.ACK) {
                        this.state = TCPState.CLOSED;
                    } else {
                        this.seq -= 1;
                        this.ack -= 1;
                        this.state = TCPState.LAST_ACK;
                    }
                    break;
                case CLOSED:
                    System.out.println("Connection closed");
                    return;
            }
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