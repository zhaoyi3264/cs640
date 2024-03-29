package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.MACAddress;

import java.util.Arrays;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
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
		/* Handle packets                                                   */
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4) {
			return;
		}
		// get header 
		IPv4 header = (IPv4) etherPacket.getPayload();
		short checksum = header.getChecksum();
		// compute checksum
		header.setChecksum((short)0);
		byte[] data = header.serialize();
		header = (IPv4) header.deserialize(data, 0, data.length);
		if (checksum != header.getChecksum()) {
			return;
		}
		// decrement ttl
		byte ttl = (byte)(header.getTtl() - 1);
		if (ttl <= 0) {
			return;
		}
		header.setTtl(ttl);
		// check if destination is one of the interface
		int dstAddr = header.getDestinationAddress();
		for (Iface iface : this.interfaces.values()) { 
			if (iface.getIpAddress() == dstAddr) {
				return;
			}
		}
		// forward
		RouteEntry re = routeTable.lookup(dstAddr);
		if (re == null) {
			return;
		}
		if (re.getGatewayAddress() != 0) {
			dstAddr = re.getGatewayAddress();
		}
		ArpEntry arpe = arpCache.lookup(dstAddr);
		if (arpe == null) {
			return;
		}
		// compute new checksum
		header.setChecksum((short)0);
		data = header.serialize();
		header = (IPv4) header.deserialize(data, 0, data.length);
		etherPacket.setPayload(header);
		// update MAC address in the frame
		String src = re.getInterface().getMacAddress().toString();
		String dst = arpe.getMac().toString();
		etherPacket.setSourceMACAddress(src);
		etherPacket.setDestinationMACAddress(dst);
		this.sendPacket(etherPacket, re.getInterface());
		/********************************************************************/
	}
}
