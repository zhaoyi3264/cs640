import java.net.*;

public class Test {
    public static void main(String[] args) {
        InetAddress remoteAddress = null;
        try {
            remoteAddress = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
		byte[] data = {};
		DatagramPacket p = TCP.createPacket(remoteAddress, 8000, 0, 0, TCP.calculateFlags(1,1,0), data);
        System.out.println(TCP.asTCP(p));
    }
}