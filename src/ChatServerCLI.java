import java.io.*;
import java.net.*;
import java.util.HashMap;

import me.alexpanov.net.FreePortFinder;

public class ChatServerCLI {
	private static HashMap<String, PrintWriter> connectedClients = new HashMap<>();
	private static final int MAX_CONNECTED = 50;
	private static int PORT;
	private static boolean verbose;
	private static ServerSocket server;

	// Start of Client Handler
	private static class ClientHandler implements Runnable {
		private Socket socket;
		private PrintWriter out;
		private BufferedReader in;
		private String name;

		public ClientHandler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run(){
			if (verbose)
				System.out.println("Kliens csatlakozott: " + socket.getInetAddress());
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				for(;;) {
					out.println("Adj meg felhasznalonevet:\t");
					name = in.readLine();
					if (name == null) {
						return;
					}
					synchronized (connectedClients) {
						if (!name.isEmpty() && !connectedClients.keySet().contains(name)) break;
						else out.println("HIBASNEV");
					}
				}
				out.println("Udv a chat szobaban, " + name.toUpperCase() + "!");
				if (verbose) System.out.println(name.toUpperCase() + " belepett.");
				broadcastMessage("[SERVER UZENET] " + name.toUpperCase() + " belepett.");
				connectedClients.put(name, out);
				String message;
				out.println("Mostmar elkezdhetsz chatelni...");
				while ((message = in.readLine()) != null) {
					if (!message.isEmpty()) {
						if (message.toLowerCase().equals("/quit")) break;
						broadcastMessage(name + ": " + message);
					}
				}
			} catch (Exception e) {
				if (verbose) System.out.println(e);
			} finally {
				if (name != null) {
					if (verbose) System.out.println(name + " tavozik");
					connectedClients.remove(name);
					broadcastMessage(name + " tavozott");
				}
			}
		}

	}

	private static void broadcastMessage(String message) {
		for (PrintWriter p: connectedClients.values()) {
			p.println(message);
		}
	}

	public static void start(boolean isVerbose) {
		verbose = isVerbose;
		try {
			server = new ServerSocket(getRandomPort());
			if (verbose) {
				System.out.println("Elindult a szerver ezen a porton: " + PORT);
				System.out.println("Csatlakozasra varok...");
			}
			for(;;) {
				if (connectedClients.size() <= MAX_CONNECTED){
					Thread newClient = new Thread(
							new ClientHandler(server.accept()));
					newClient.start();
				}
			}
		}
		catch (Exception e) {
			if (verbose) {
				System.out.println("\nHiba tortent: \n");
				e.printStackTrace();
				System.out.println("\nKilepes...");
			}
		}
	}

	public static void stop() throws IOException {
		if (!server.isClosed()) server.close();
	}
	
	private static int getRandomPort() {
		int port = FreePortFinder.findFreeLocalPort();
		PORT = port;
		return port;
	}

	public static void main(String[] args) throws IOException {
		start(args[0].toLowerCase().equals("verbose") ? true : false);
	}
}