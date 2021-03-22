package edu.wisc.cs.sdn.vnet.rt;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

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
	private ConcurrentHashMap<Integer, RIPv2Entry> localRIP;

	// infinite distance
	private static final int INF_DIST = 16;

	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
		this.localRIP = new ConcurrentHashMap<Integer, RIPv2Entry>();
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
			// TODO: add rip table entry
			// direct connected hosts are in subnet, thus has cost 0 and no gateway(nextHop)
			RIPv2Entry ripEntry = new RIPv2Entry(subnetAddr, mask, 0);
			ripEntry.setNextHopAddress(0);
			this.localRIP.put(subnetAddr, ripEntry);
			// send RIP request to all interfaces
			this.sendRip(RipType.REQ, null, iface);
		}
		Timer timer = new Timer(true);
		// unsolicited RIP response every 10 seconds
		TimerTask unsol = new TimerTask() {
			@Override
			public void run() {
				System.out.println("Sending UNSOLICITED RESPONSE...");
				for (Iface iface : interfaces.values()) {
					sendRip(RipType.UNSOL, null, iface);
				}
			}
		};
		timer.scheduleAtFixedRate(unsol, 0, 10000);
		// TODO: time out entries that are not updated in last 30 seconds
		// list of ip addresses with corresponding entries to be removed
		/*
		TimerTask timeOut = new TimerTask(){
			@Override
			public void run(){
				ArrayList<Integer> ips = new ArrayList<Integer>();
				for (Map.Entry element : localRIP.entrySet()) {
					RIPv2Entry entry = (RIPv2Entry) element.getValue();
					// remove if is not direct neighbor (cost != 1)
					if (System.currentTimeMillis() - entry.getTime() > 30000 && entry.getMetric() != 1) {
						ips.add(entry.getAddress());
					}
				}

				for (Integer addr : ips) {
					RIPv2Entry entry = localRIP.get(addr);
					int mask = entry.getSubnetMask();
					localRIP.remove(addr);
					routeTable.remove(addr, mask);
					System.out.print("Timing out entry of " + IPv4.fromIPv4Address(addr));
				}
			}
		};
		*/
		timer.scheduleAtFixedRate(timeOut, 0, 1000);
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
		if (ipPacket.getDestinationAddress() != IPv4.toIPv4Address("224.0.0.9")) {
			return null;
		}
		// Make sure it's UDP
		if (ipPacket.getProtocol() != IPv4.PROTOCOL_UDP) {
			return null;
		}
		UDP UdpPacket = (UDP)ipPacket.getPayload();
		// Verify rip port
		if (UdpPacket.getDestinationPort() != UDP.RIP_PORT) {
			return null;
		}
		RIPv2 rip = (RIPv2)UdpPacket.getPayload();
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
				// TODO: parse response
				for (RIPv2Entry ripEntry : rip.getEntries()) {
				    int addr = ripEntry.getAddress();
				    int mask = ripEntry.getSubnetMask();
				    // 16 indicates infinite distance
				    int metric = Math.min(INF_DIST, ripEntry.getMetric() + 1);
				    int nextHop = ip.getSourceAddress();

				    RIPv2Entry localEntry= this.localRIP.get(addr);

				    if (localEntry == null) {
				        if (metric < INF_DIST) {
				            this.localRIP.put(addr, new RIPv2Entry(addr, mask, metric));
				            this.routeTable.insert(addr, nextHop, mask, iface);
					    System.out.print("Inserting entry of " + IPv4.fromIPv4Address(addr));
				        }
				        // otherwise, do nothing
				    } else {
					    if (localEntry.getMetric() > metric) {
			                            // update the distance
                        			    this.localRIP.replace(addr, new RIPv2Entry(addr, mask, metric));
						    this.routeTable.update(addr, mask, nextHop, iface);
						    System.out.print("Updating entry of " + IPv4.fromIPv4Address(addr));
					    }
					    // if a link fails, remove it if it corresponds to the same interface (if found)
					    if (metric >= INF_DIST) {
						    RouteEntry bestEntry = this.routeTable.lookup(addr);
						    if (bestEntry != null && iface.equals(bestEntry.getInterface())) {
							    this.localRIP.remove(addr);
							    this.routeTable.remove(addr, mask);
							    System.out.print("Link to " + IPv4.fromIPv4Address(addr) + "is down, deleting entry...");
						    }
					    }

				    }
				}
				// broadcast the updates (if any) to other routers immediately
				for (Iface iFace : this.interfaces.values()) {
					sendRip(RipType.UNSOL, null, iFace);
				}
				break;
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
		ip.setTtl((byte)64);
		ip.setVersion((byte)4);

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
			//TODO: add rip table to rip
			for (Map.Entry element : localRIP.entrySet()) {
			    RIPv2Entry entry = (RIPv2Entry) element.getValue();
			    int addr = entry.getAddress();
			    int mask = entry.getSubnetMask();
			    int metric = entry.getMetric();
			    int nextHop = iface.getIpAddress();
			    RIPv2Entry ripEntry = new RIPv2Entry(addr, mask, metric);
			    ripEntry.setNextHopAddress(nextHop);
			    rip.addEntry(ripEntry);
			}
		}

		//TODO: add rip entries to rip
		udp.setPayload(rip);
		ip.setPayload(udp);
		ether.setPayload(ip);
		// ether.serialize();

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
