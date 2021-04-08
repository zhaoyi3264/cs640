package com.cs640.tcp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCP {
	
	public static final int HLEN = 24;
	
	private int seq;
	private int ack;
	private long timestamp;
	private int length;
	private boolean SYN;
	private boolean FIN;
	private boolean ACK;
	private int checksum;
	private byte[] data;
	
	public TCP (int seq, int ack, long timestamp, int length, boolean SYN,
				boolean FIN, boolean ACK, int checksum, byte[] data) {
		this.seq = seq;
		this.ack = ack;
		this.timestamp = timestamp;
		this.length = length;
		this.SYN = SYN;
		this.FIN = FIN;
		this.ACK = ACK;
		this.checksum = checksum;
		this.data = data;
	}
	
	public static TCP asTCP(DatagramPacket packet) {
		int length = packet.getLength() - TCP.HLEN;
		ByteBuffer bb = ByteBuffer.allocate(TCP.HLEN + length).wrap(packet.getData());
		// seq
		int seq = bb.getInt();
		// ack
		int ack = bb.getInt();
		// timestamp
		long timestamp = bb.getLong();
		// length and flags
		int l = bb.getInt();
		assert l >> 3 == length;
		boolean SYN = false;
		boolean FIN = false;
		boolean ACK = false;
		// checksum
		int checksum = bb.getInt();
		// data
		byte[] data = new byte[length];
		bb.get​(data, 0, length);
		return new TCP(seq, ack, timestamp, length, SYN, FIN, ACK, checksum, data);
	}
	
	public String toString() {
		return String.format("seq=%d, ack=%d, timestamp=%d, length=%d, checksum=%d, ",
				seq, ack, timestamp, length, checksum)
			+ String.format("SYN=%b, FIN=%b, ACK=%b, Data=", SYN, FIN, ACK)
			+ Arrays.toString(data);
	}
}
