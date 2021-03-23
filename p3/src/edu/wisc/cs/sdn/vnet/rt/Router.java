package edu.wisc.cs.sdn.vnet.rt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.MACAddress;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.packet.RIPv2;
import net.floodlightcontroller.packet.RIPv2Entry;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;

	/** ARP cache for the router */
	private ArpCache arpCache;

	// Create a local RIP ConcurrentHashMap
	private ConcurrentHashMap<Integer, RipEntry> ripTable;

	// infinite distance
	private static final int MAX_DIST = 16;

	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
		this.ripTable = new ConcurrentHashMap<>();
	}

	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }

	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}

		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}

	public void startRip() {
		for (Iface iface : this.interfaces.values()) {
			// add entries for the directly reachable subnets
			int mask = iface.getSubnetMask();
			int subnetAddr = iface.getIpAddress() & mask;
			this.routeTable.insert(subnetAddr, 0, mask, iface);
			// direct connected hosts are in subnet, thus has cost 0 and no gateway(nextHop)
			this.ripTable.put(subnetAddr, new RipEntry(subnetAddr, mask, 0, 0));
			// send RIP request to all interfaces
			this.sendRip(RipType.REQ, null, iface);
		}
		// unsolicited RIP response every 10 seconds
		Runnable unsol = new Runnable() {
			@Override
			public void run() {
				while (true) {
					System.out.println("Sending UNSOLICITED RESPONSE...");
					for (Iface iface : interfaces.values()) {
						sendRip(RipType.UNSOL, null, iface);
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		Runnable purge = new Runnable(){
			@Override
			public void run(){
				while (true) {
					Iterator<Map.Entry<Integer, RipEntry>> iterator = ripTable.entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry<Integer, RipEntry> entry = iterator.next();
						RipEntry ripEntry = entry.getValue();
						boolean expired = System.currentTimeMillis() - ripEntry.timeStamp >= 30000;
						if (ripEntry.metric != 0 && expired) {
							System.out.println("Removing expired entry...");
							System.out.println("\tdest:" +  IPv4.fromIPv4Address(ripEntry.addr));
							System.out.println("\tmetric:" + ripEntry.metric);
							iterator.remove();
							routeTable.remove(ripEntry.addr, ripEntry.mask);
						}
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		Thread t1 = new Thread(unsol);
		Thread t2 = new Thread(purge);
		t1.setDaemon(true);
		t2.setDaemon(true);
		t1.start();
		t2.start();
		System.out.println("RIP initialized...");
	}

	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}

		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));

		/********************************************************************/
		/* TODO: Handle packets                                             */

		switch(etherPacket.getEtherType())
		{
		case Ethernet.TYPE_IPv4:
			RIPv2 rip = this.getRipPacket(etherPacket);
			if (rip != null) {
				this.handleRipPacket(rip.getCommand(), etherPacket, inIface);
			} else {
				this.handleIpPacket(etherPacket, inIface);
			}
			break;
		// Ignore all other packet types, for now
		}
		/********************************************************************/
	}

	private RIPv2 getRipPacket(Ethernet etherPacket) {
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
		// Make sure it's UDP
		if (ipPacket.getProtocol() != IPv4.PROTOCOL_UDP) {
			return null;
		}
		UDP udpPacket = (UDP)ipPacket.getPayload();
		// Verify rip port
		if (udpPacket.getDestinationPort() != UDP.RIP_PORT) {
			return null;
		}
		RIPv2 rip = (RIPv2)udpPacket.getPayload();
		return rip;
	}

	private void handleRipPacket(byte command, Ethernet etherPacket, Iface iface) {
		IPv4 ip = (IPv4)etherPacket.getPayload();
		UDP udp = (UDP)ip.getPayload();
		RIPv2 rip = (RIPv2)udp.getPayload();

		switch(command) {
			case RIPv2.COMMAND_REQUEST:
				System.out.println("Handling RIP request...");
				sendRip(RipType.RES, etherPacket, iface);
				break;
			case RIPv2.COMMAND_RESPONSE:
				System.out.println("Handling RIP response from " + IPv4.fromIPv4Address(ip.getSourceAddress()));
				this.handleRipResponse(ip, rip, iface);
				System.out.println(this.routeTable);
				break;
		}
	}
	
	private void handleRipResponse(IPv4 ip, RIPv2 rip, Iface iface) {
		for (RIPv2Entry entry : rip.getEntries()) {
			int subnetAddr = entry.getAddress();
			int mask = entry.getSubnetMask();
			int nextHop = ip.getSourceAddress();
			int metric = Math.min(Router.MAX_DIST, entry.getMetric() + 1);
			
			RipEntry ripEntry = this.ripTable.get(subnetAddr);
			if (ripEntry == null) {
				if (metric < Router.MAX_DIST) {
					System.out.println("Creating RIP entry..."
						+ "\n\tdest: " + IPv4.fromIPv4Address(subnetAddr)
						+ "\n\tnextHop: " + IPv4.fromIPv4Address(nextHop)
						+ "\n\tmetric: " + metric);
					this.ripTable.put(subnetAddr, new RipEntry(subnetAddr, mask, nextHop, metric));
					this.routeTable.insert(subnetAddr, nextHop, mask, iface);
				}
			} else {
				if (metric <= ripEntry.metric) {
					System.out.println("Updating RIP entry..."
						+ "\n\tdest: " + IPv4.fromIPv4Address(subnetAddr)
						+ "\n\tnextHop: " + IPv4.fromIPv4Address(nextHop)
						+ "\n\tmetric: " + metric);
					this.ripTable.replace(subnetAddr, new RipEntry(subnetAddr, mask, nextHop, metric));
					this.routeTable.update(subnetAddr, mask, nextHop, iface);
				}

				if (metric >= Router.MAX_DIST) {
					System.out.println("Deleting RIP entry..."
						+ "\n\tdest: " + IPv4.fromIPv4Address(subnetAddr));
					this.routeTable.remove(subnetAddr, mask);
					this.ripTable.remove(subnetAddr);
				}
			}
		}
	}

	private void sendRip(RipType type, Ethernet etherPacket, Iface iface) {
		RIPv2 rip = new RIPv2();
		UDP udp = new UDP();
		IPv4 ip = new IPv4();
		Ethernet ether = new Ethernet();

		udp.setSourcePort(UDP.RIP_PORT);
		udp.setDestinationPort(UDP.RIP_PORT);

		ip.setSourceAddress(iface.getIpAddress());
		ip.setProtocol(IPv4.PROTOCOL_UDP);
		ip.setTtl((byte)16);

		ether.setSourceMACAddress(iface.getMacAddress().toString());
		ether.setEtherType(Ethernet.TYPE_IPv4);

		switch(type) {
			case REQ:
				System.out.println("Sending RIP request...");
				rip.setCommand(RIPv2.COMMAND_REQUEST);
				ip.setDestinationAddress("224.0.0.9");
				ether.setDestinationMACAddress("FF:FF:FF:FF:FF:FF");
				break;
			case UNSOL:
				rip.setCommand(RIPv2.COMMAND_RESPONSE);
				ip.setDestinationAddress("224.0.0.9");
				ether.setDestinationMACAddress("FF:FF:FF:FF:FF:FF");
				break;
			case RES:
				System.out.println("Sending RIP response...");
				rip.setCommand(RIPv2.COMMAND_RESPONSE);
				IPv4 ipSrc = (IPv4)etherPacket.getPayload();
				ip.setDestinationAddress(ipSrc.getSourceAddress());
				ether.setDestinationMACAddress(etherPacket.getSourceMACAddress());
				break;
		}

		if (type == RipType.RES || type == RipType.UNSOL) {
			for (Map.Entry<Integer, RipEntry> entry : this.ripTable.entrySet()) {
				RipEntry ripEntry = entry.getValue();
				RIPv2Entry tmp = new RIPv2Entry(ripEntry.addr, ripEntry.mask, ripEntry.metric);
				tmp.setNextHopAddress(ripEntry.nextHop);
				rip.addEntry(tmp);
			}
		}

		udp.setPayload(rip);
		ip.setPayload(udp);
		ether.setPayload(ip);
		this.sendPacket(ether, iface);
	}

	private void handleIpPacket(Ethernet etherPacket, Iface inIface)
	{
		// Make sure it's an IP packet
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
		{ return; }

		// Get IP header
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
		System.out.println("Handle IP packet");

		// Verify checksum
		short origCksum = ipPacket.getChecksum();
		ipPacket.resetChecksum();
		byte[] serialized = ipPacket.serialize();
		ipPacket.deserialize(serialized, 0, serialized.length);
		short calcCksum = ipPacket.getChecksum();
		if (origCksum != calcCksum)
		{ return; }

		// Check TTL
		ipPacket.setTtl((byte)(ipPacket.getTtl()-1));
		if (0 == ipPacket.getTtl())
		{ return; }

		// Reset checksum now that TTL is decremented
		ipPacket.resetChecksum();

		// Check if packet is destined for one of router's interfaces
		for (Iface iface : this.interfaces.values())
		{
			if (ipPacket.getDestinationAddress() == iface.getIpAddress())
			{ return; }
		}

		// Do route lookup and forward
		this.forwardIpPacket(etherPacket, inIface);
	}

	private void forwardIpPacket(Ethernet etherPacket, Iface inIface)
	{
		// Make sure it's an IP packet
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
		{ return; }
		System.out.println("Forward IP packet");

		// Get IP header
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
		int dstAddr = ipPacket.getDestinationAddress();

		// Find matching route table entry 
		RouteEntry bestMatch = this.routeTable.lookup(dstAddr);

		// If no entry matched, do nothing
		if (null == bestMatch)
		{ return; }

		// Make sure we don't sent a packet back out the interface it came in
		Iface outIface = bestMatch.getInterface();
		if (outIface == inIface)
		{ return; }

		// Set source MAC address in Ethernet header
		etherPacket.setSourceMACAddress(outIface.getMacAddress().toBytes());

		// If no gateway, then nextHop is IP destination
		int nextHop = bestMatch.getGatewayAddress();
		if (0 == nextHop)
		{ nextHop = dstAddr; }

		// Set destination MAC address in Ethernet header
		ArpEntry arpEntry = this.arpCache.lookup(nextHop);
		if (null == arpEntry)
		{ return; }
		etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());

		this.sendPacket(etherPacket, outIface);
	}
}

enum RipType {
	UNSOL, REQ, RES
}

class RipEntry {
	int addr;
	int mask;
	int nextHop;
	int metric;
	long timeStamp;
	
	public RipEntry(int addr, int mask, int nextHop, int metric) {
		this.addr = addr;
		this.mask = mask;
		this.nextHop = nextHop;
		this.metric = metric;
		this.timeStamp = System.currentTimeMillis();
	}
}

