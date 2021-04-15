import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCPend {
    public static void main(String[] args) {
        // -p <port> -s <remote IP> -a <remote port> -f <file name> -m <mtu> -c <sws>
        if (args.length == 12 && args[0].equals("-p") && args[2].equals("-s") &&
            args[4].equals("-a") && args[6].equals("-f") && args[8].equals("-m")
            && args[10].equals("-c")) {
            
            int port = Integer.parseInt(args[1]);
            InetAddress remoteAddress = null;
            try {
                remoteAddress = InetAddress.getByName(args[3]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            int remotePort = Integer.parseInt(args[5]);
            String file = args[7];
            int mtu = Integer.parseInt(args[9]);
            int sws = Integer.parseInt(args[11]);
            sender(port, remoteAddress, remotePort, file, mtu, sws);
        }
        // -p <port> -m <mtu> -c <sws> -f <file name>
        else if (args.length == 8 && args[0].equals("-p") && args[2].equals("-m") &&
            args[4].equals("-c") && args[6].equals("-f")) {
            int port = Integer.parseInt(args[1]);
            int mtu = Integer.parseInt(args[3]);
            int sws = Integer.parseInt(args[5]);
            String file = args[7];
            receiver(port, mtu, sws, file);
        }
        // error
        else {
            System.out.println("usage:\n"
                + "java TCPend -p <port> -s <remote IP> -a <remote port> -f <file name> -m <mtu> -c <sws>\n"
                + "java TCPend -p <port> -m <mtu> -c <sws> -f <file name>");
            System.exit(1);
        }
    }

    public static void sender(int port, InetAddress remoteAddress, int remotePort,
        String file, int mtu, int sws) {
        Sender sender = new Sender(port, mtu, sws, file, remoteAddress, remotePort);
        sender.connect();
        // sender.run();
    }

    public static void receiver(int port, int mtu, int sws, String file) {
        Receiver receiver = new Receiver(port, mtu, sws, file);
        receiver.connect();
        // receiver.run();
    }
}
