package com.cs640.tcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.cs640.tcp.TCP;

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
	
	public static DatagramPacket createPacket(byte[] data, InetAddress address, int port) {
		// TODO: add flags
		int length = data.length;
		ByteBuffer bb = ByteBuffer.allocate(TCP.HLEN + length);
		// seq
		bb.putInt(3136);
		// ack
		bb.putInt(9908);
		// timestamp
		bb.putLong(System.nanoTime());
		// length and flags
		bb.putInt(length << 3);
		// checksum
		bb.putInt(0);
		// data
		bb.put(data);
		byte[] buf = bb.array();
		// TODO: compute checksum
		return new DatagramPacket(buf, 0, TCP.HLEN + length, address, port);
	}
	
	public static void sender(int port, InetAddress remoteAddress, int remotePort,
		String file, int mtu, int sws) {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		socket.connect​(remoteAddress, remotePort);
		byte[] data = {1,2,3,4,5};
		DatagramPacket p = createPacket(data, remoteAddress, remotePort);
		try {
			socket.send(p);
			System.out.println("Packet sent");

			byte[] buf = new byte[32];
			p = new DatagramPacket(buf, buf.length);
			socket.receive(p);
			TCP tcp = TCP.asTCP(p);
			System.out.println(tcp);
			System.out.println("Packet received");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void receiver(int port, int mtu, int sws, String file) {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		byte[] buf = new byte[32];
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		try {
			socket.receive(p);
			TCP tcp = TCP.asTCP(p);
			System.out.println(tcp);
			System.out.println("Packet received");

			InetAddress remoteAddress = p.getAddress();
			int remotePort = p.getPort();
			socket.connect​(remoteAddress, remotePort);
			byte[] data = {5,4,3,2,1};
			p = createPacket(data, remoteAddress, remotePort);
			socket.send(p);
			System.out.println("Packet sent");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void test() {
		InetAddress address = null;
		try {
			address = InetAddress.getLocalHost();
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[] data = {12, 16, 19, 99, 20, 21, 12};
		DatagramPacket p = createPacket(data, address, 8888);
		TCP tcp = TCP.asTCP(p);
		System.out.println(tcp);
	}
}
