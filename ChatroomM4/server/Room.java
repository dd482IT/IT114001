package server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Room implements AutoCloseable {
	private static SocketServer server;// used to refer to accessible server functions
	private String name;
	private final static Logger log = Logger.getLogger(Room.class.getName());

	// Commands
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";
	private final static String ROLL = "roll";
	private final static String FLIP = "flip";
	private final static String BOLD = "bold";
	private final static String ITALIC = "italic";
	private final static String MUTE = "mute";
	private final static String UNMUTE = "unmute";
	Random rand = new Random();

	public Room(String name) {
		this.name = name;
	}

	public static void setServer(SocketServer server) {
		Room.server = server;
	}

	public String getName() {
		return name;
	}

	private List<ServerThread> clients = new ArrayList<ServerThread>();

	protected synchronized void addClient(ServerThread client) {
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			log.log(Level.INFO, "Attempting to add a client that already exists");
		} else {
			clients.add(client);
			if (client.getClientName() != null) {
				client.sendClearList();
				sendConnectionStatus(client, true, "joined the room " + getName());
				updateClientList(client);
			}
			/**
			 * try { client.importFile(); } catch (IOException e) { e.printStackTrace();
			 * log.log(Level.INFO, "Error Attempting to import a file"); }
			 **/
		}
	}

	private void updateClientList(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c != client) {
				boolean messageSent = client.sendConnectionStatus(c.getClientName(), true, null);
			}
		}
	}

	protected synchronized void removeClient(ServerThread client) {
		clients.remove(client);
		if (clients.size() > 0) {
			// sendMessage(client, "left the room");
			sendConnectionStatus(client, false, "left the room " + getName());
		} else {
			cleanupEmptyRoom();
		}
	}

	private void cleanupEmptyRoom() {
		// If name is null it's already been closed. And don't close the Lobby
		if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
			return;
		}
		try {
			log.log(Level.INFO, "Closing empty room: " + name);
			close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void joinRoom(String room, ServerThread client) {
		server.joinRoom(room, client);
	}

	protected void joinLobby(ServerThread client) {
		server.joinLobby(client);
	}

	/***
	 * Helper function to process messages to trigger different functionality.
	 * 
	 * @param message The original message being sent
	 * @param client  The sender of the message (since they'll be the ones
	 *                triggering the actions)
	 */
	private boolean processCommands(String message, ServerThread client) {
		boolean wasCommand = false;
		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
				String[] comm = message.split(COMMAND_TRIGGER);
				log.log(Level.INFO, message);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				if (command != null) {
					command = command.toLowerCase();
				}
				String roomName;
				switch (command) {
				case CREATE_ROOM:
					roomName = comm2[1];
					if (server.createNewRoom(roomName)) {
						joinRoom(roomName, client);
					}
					wasCommand = true;
					break;
				case JOIN_ROOM:
					roomName = comm2[1];
					joinRoom(roomName, client);
					wasCommand = true;
					break;
				case ROLL:
					int dice1 = rand.nextInt(5) + 1;
					int dice2 = rand.nextInt((11 - 5)) + 1;
					sendMessage(client, "*... rolled a: *" + (dice1 + dice2));
					wasCommand = true;
					break;
				case FLIP:
					int coin = rand.nextInt(1);
					String side;
					if (coin == 1)
						side = "heads";
					else
						side = "tails";
					sendMessage(client, "*...flipped a coin and got *" + side);
					wasCommand = true;
					break;
				case MUTE:
					String[] clientList = message.split(" ");
					clientList[0] = null;
					for (int i = 0; i < clientList.length; i++) {
						if (clientList[i] != null) {
							// client.mutedList.add(clientList[i]);
							client.addMuted(clientList[i]); // added this on 12/12/2020
							sendMessage(client, "#muted " + clientList[i] + "#");
						} else {
							log.log(Level.INFO, clientList[i] + "NOT MUTED");
						}
					}
					wasCommand = true;
					break;
				case UNMUTE:
					clientList = message.split(" ");
					clientList[0] = null;
					for (int i = 0; i < clientList.length; i++) {
						if (clientList[i] != null) {
							// client.mutedList.remove(clientList[i]);
							client.removeMuted(clientList[i]); // added this on 12/12/2020
							sendMessage(client, "#Unmuted " + clientList[i] + "#");
						} else {
							log.log(Level.INFO, clientList[i] + "NOT UNMUTED");
						}
					}
					wasCommand = true;
					break;

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wasCommand;
	}

	// TODO changed from string to ServerThread
	protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
			if (!messageSent) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + c.getId());
			}
		}
	}

	/***
	 * Takes a sender and a message and broadcasts the message to all clients in
	 * this room. Client is mostly passed for command purposes but we can also use
	 * it to extract other client info.
	 * 
	 * @param sender  The client sending the message
	 * @param message The message to broadcast inside the room
	 */

	/*
	 * protected void sendMessage(ServerThread sender, String message) {
	 * log.log(Level.INFO, getName() + ": Sending message to " + clients.size() +
	 * " clients"); if (processCommands(message, sender)) { // it was a command,
	 * don't broadcast return; } Iterator<ServerThread> iter = clients.iterator();
	 * while (iter.hasNext()) { ServerThread client = iter.next(); boolean
	 * messageSent = client.send(sender.getClientName(), message); if (!messageSent)
	 * { iter.remove(); log.log(Level.INFO, "Removed client " + client.getId()); } }
	 * }
	 */

	protected void sendPrvMessag(ServerThread sender, String message) {

		// checks if message is meant to be private, finds space to pull the name of the
		// user
		int space = 0;
		if (message.contains("@") && message.indexOf("@") == 0) {

			for (int i = 0; i < message.length(); i++) {
				if (message.charAt(i) == ' ') {
					space = i;
					break;
				}
			}
			// Receiver holds the name of the client
			String reciever = message.substring(1, space); // sets the receiver
			String messageTo = " `Whisper: " + message.substring(space, message.length()) + "` ";
			log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
			if (processCommands(message, sender)) {
				// it was a command, don't broadcast
				return;
			}
			// loops through clients, compares receiver to the name of each client
			Iterator<ServerThread> iter = clients.iterator();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				if (client.getClientName().equals(reciever)) {
					boolean messageSent = client.send(sender.getClientName(), messageTo);
					boolean orignal = sender.send(sender.getClientName(), message);
					if (!messageSent && !orignal) {
						iter.remove();
						log.log(Level.INFO, "Removed client " + client.getId());
					} else {
						break;
					}
				}
			}
		} else {

			log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
			if (processCommands(message, sender)) {
				// it was a command, don't broadcast
				return;
			}
			Iterator<ServerThread> iter = clients.iterator();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client " + client.getId());
				}
			}

		}
	}

	protected void sendMessage(ServerThread sender, String message) {
		log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");

		if (processCommands(message, sender)) {
			// it was a command, don't broadcast
			return;
		}

		if (message.contains("@")) {
			Iterator<ServerThread> iter = clients.iterator();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				if (message.contains("@" + client.getClientName())) {
					String[] messageSplit = message.split(" ");
					String newMessage = "`Whisper: `";
					for (int i = 0; i < messageSplit.length; i++) {
						String word = messageSplit[i];
						if (!word.contains("@")) {
							newMessage = newMessage + " " + word;
						}
					}
					boolean messageSent = client.send(sender.getClientName(), newMessage);
					boolean messageRepeated = sender.send(sender.getClientName(),
							newMessage + "`[sent to " + client.getClientName() + "]`");
					if (!messageSent || !messageRepeated) {
						iter.remove();
						log.log(Level.INFO, "Removed client " + client.getId());
					}
				}
			}
		} else {
			Iterator<ServerThread> iter = clients.iterator();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				if (!client.isMuted(sender.getClientName())) {
					boolean messageSent = client.send(sender.getClientName(), message);
					if (!messageSent) {
						iter.remove();
						log.log(Level.INFO, "Removed client " + client.getId());
					}
				} else {
					log.log(Level.INFO, "ERROR");
				}

			}
		}

	}

	// -------------------------------------------------------------------------

	// -------------------------------------------------------------------------
	public List<String> getRooms() {
		return server.getRooms();
	}

	/***
	 * Will attempt to migrate any remaining clients to the Lobby room. Will then
	 * set references to null and should be eligible for garbage collection
	 */
	@Override
	public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
			log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
			Iterator<ServerThread> iter = clients.iterator();
			Room lobby = server.getLobby();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				lobby.addClient(client);
				iter.remove();
			}
			log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
		}
		server.cleanupRoom(this);
		name = null;
		// should be eligible for garbage collection now
	}

}