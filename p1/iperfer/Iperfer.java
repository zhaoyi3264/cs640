import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;

public class Iperfer {
	public static void main(String[] args) {
		int argc = args.length;
		if (argc == 7) {
			// java Iperfer -c -h <server hostname> -p <server port> -t <time>
			if (!args[0].equals("-c") || !args[1].equals("-h") ||
				!args[3].equals("-p") || !args[5].equals("-t")) {
				exit();
			}
			String host = args[2];
			int port = parsePort(args[4]);
			int time = Integer.parseInt(args[6]);
			client(host, port, time);
		} else if (argc == 3) {
			// java Iperfer -s -p <listen port>
			if (!args[0].equals("-s") || !args[1].equals("-p")) {
				exit();
			}
			int port = parsePort(args[2]);
			server(port);
		} else {
			exit();
		}
	}

	public static void exit() {
		System.out.println("Error: missing or additional arguments");
		System.exit(1);
	}

	public static int parsePort(String port) {
		int p = Integer.parseInt(port);
		if (p < 1024 || p > 65535) {
			System.out.println("Error: port number must be in the range 1024 to 65535");
			System.exit(1);
		}
		return p;
	}

	public static void client(String host, int port, int time) {
		long end = 0;
		byte[] buffer = new byte[1000];
		Perf p = new Perf();
		Socket client = null;
		try {
			client = new Socket(host, port);
			OutputStream out = client.getOutputStream();
			end = System.currentTimeMillis() + time * 1000;
			p.start();
			while (System.currentTimeMillis() < end) {
				out.write(buffer);
				out.flush();
				p.increment(1.0);
			}
			p.end();
			out.close();
			client.close();
		} catch (Exception e) {
			System.out.println("Error: cannot connect to server");
			System.exit(1);
		}
		System.out.printf("sent=%d KB rate=%.3f Mbps\n", (int)p.getKb(), p.getRate());
	}

	public static void server(int port) {
		byte[] buffer = new byte[1000];
		double len = 0.0;
		Perf p = new Perf();
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
			Socket client = server.accept();
			InputStream in = client.getInputStream();
			p.start();
			while ((len = in.read(buffer, 0, 1000)) != -1) {
				p.increment(len / 1000.0);
			}
			p.end();
			in.close();
			client.close();
			server.close();
		} catch (Exception e) {
			System.out.println("Error: cannot connect to client");
			System.exit(1);
		}
		System.out.printf("received=%d KB rate=%.3f Mbps\n", (int)p.getKb(), p.getRate());
	}
}

class Perf {
	private double kb = 0.0;
	private double rate = 0.0;
	private long t0 = 0;
	private long t1 = 0;
	
	public void start() {
		this.t0 = System.currentTimeMillis();
	}
	
	public void end() {
		this.t1 = System.currentTimeMillis();
	}
	
	public void increment(double d) {
		this.kb += d;
	}
	
	public double getKb() {
		return this.kb;
	}
	
	public double getRate() {
		return this.kb / 1000 * 8 / ((this.t1 - this.t0) / 1000.0);
	}
}
