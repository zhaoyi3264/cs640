package edu.wisc.cs.sdn.vnet.sw;

import java.lang.Runnable;
import java.lang.Thread;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{

	private ConcurrentHashMap<String, Entry> table;

	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.table = new ConcurrentHashMap<>();
		Runnable r = new Runnable () {
			@Override
			public void run() {
				try {
					while (true) {
						Entry e = null;
						for (String key : table.keySet()) {
							e = table.get(key);
							if (System.currentTimeMillis() - e.timestamp >= 15000) {
								table.remove(key);
							}
						}
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.start();
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
		String src = etherPacket.getSourceMAC().toString();
		String dst = etherPacket.getDestinationMAC().toString();
		// record the source MAC and the incoming interface pair
		Entry srcEntry = table.get(src);
		if (srcEntry != null) { // update
			srcEntry.iface = inIface;
			srcEntry.timestamp = System.currentTimeMillis();
		} else { // create 
			table.put(src, new Entry(inIface, System.currentTimeMillis()));
		}
		// send the packet
		Entry dstEntry = table.get(dst);
		if (dstEntry != null) {
			this.sendPacket(etherPacket, dstEntry.iface);
		} else { // broadcasting to all interfaces except the incoming one
			for (Iface iface : this.interfaces.values()) {
				if (!iface.equals(inIface)) {
					this.sendPacket(etherPacket, iface);
				}
			}
		}
		/********************************************************************/
	}
}

class Entry {
	Iface iface;
	long timestamp;

	public Entry(Iface iface, long timestamp) {
		this.iface = iface;
		this.timestamp = timestamp;
	}
}
