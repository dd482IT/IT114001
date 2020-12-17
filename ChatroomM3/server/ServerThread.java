package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThread extends Thread {
	private Socket client;
	private ObjectInputStream in;// from client
	private ObjectOutputStream out;// to client
	private boolean isRunning = false;
	private Room currentRoom;// what room we are in, should be lobby by default
	private String clientName;
	public List<String> mutedList = new ArrayList<String>();
	private final static Logger log = Logger.getLogger(ServerThread.class.getName());

	public boolean isMuted(String client) {
		for (int i = 0; i < mutedList.size(); i++) {
			if (mutedList.get(i).equalsIgnoreCase(client)) {
				return true;
			}
		}
		return false;
	}

	// ***** Adding this method to add users
	public void addMuted(String clientname) {
		if (!mutedList.contains(clientname)) {
			mutedList.add(clientname);
		}
	}

	public String getClientName() {
		return clientName;
	}

	protected synchronized Room getCurrentRoom() {
		return currentRoom;
	}

	protected synchronized void setCurrentRoom(Room room) {
		if (room != null) {
			currentRoom = room;
		} else {
			log.log(Level.INFO, "Passed in room was null, this shouldn't happen");
		}
	}

	public ServerThread(Socket myClient, Room room) throws IOException {
		this.client = myClient;
		this.currentRoom = room;
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
	}

	/***
	 * Sends the message to the client represented by this ServerThread
	 * 
	 * @param message
	 * @return
	 */
	@Deprecated
	protected boolean send(String message) {
		// added a boolean so we can see if the send was successful
		try {
			out.writeObject(message);
			return true;
		} catch (IOException e) {
			log.log(Level.INFO, "Error sending message to client (most likely disconnected)");
			e.printStackTrace();
			cleanup();
			return false;
		}
	}

	/***
	 * Replacement for send(message) that takes the client name and message and
	 * converts it into a payload
	 * 
	 * @param clientName
	 * @param message
	 * @return
	 */
	protected boolean send(String clientName, String message) {
		Payload payload = new Payload();
		payload.setPayloadType(PayloadType.MESSAGE);
		payload.setClientName(clientName);
		payload.setMessage(message);

		return sendPayload(payload);
	}

	protected boolean sendConnectionStatus(String clientName, boolean isConnect, String message) {
		Payload payload = new Payload();
		if (isConnect) {
			payload.setPayloadType(PayloadType.CONNECT);
			payload.setMessage(message);
		} else {
			payload.setPayloadType(PayloadType.DISCONNECT);
			payload.setMessage(message);
		}
		payload.setClientName(clientName);
		return sendPayload(payload);
	}

	protected boolean sendClearList() {
		Payload payload = new Payload();
		payload.setPayloadType(PayloadType.CLEAR_PLAYERS);
		return sendPayload(payload);
	}

	protected boolean sendRoom(String room) {
		Payload payload = new Payload();
		// using same payload type as a response trigger
		payload.setPayloadType(PayloadType.GET_ROOMS);
		payload.setMessage(room);
		return sendPayload(payload);
	}

	private boolean sendPayload(Payload p) {
		try {
			out.writeObject(p);
			return true;
		} catch (IOException e) {
			log.log(Level.INFO, "Error sending message to client (most likely disconnected)");
			e.printStackTrace();
			cleanup();
			return false;
		}
	}

	/***
	 * Process payloads we receive from our client
	 * 
	 * @param p
	 */
	private void processPayload(Payload p) {
		switch (p.getPayloadType()) {
		case CONNECT:
			// here we'll fetch a clientName from our client
			String n = p.getClientName();
			if (n != null) {
				clientName = n;
				log.log(Level.INFO, "Set our name to " + clientName);
				if (currentRoom != null) {
					currentRoom.joinLobby(this);
				}
			}
			break;
		case DISCONNECT:
			isRunning = false;// this will break the while loop in run() and clean everything up
			break;
		case MESSAGE:
			currentRoom.sendMessage(this, p.getMessage());
			break;
		case ROLL:
			currentRoom.sendMessage(this, p.getMessage());
			break;
		case MUTE:
			currentRoom.sendMessage(this, p.getMessage());
			break;
		case GET_ROOMS:
			// far from efficient but it works for example sake
			List<String> roomNames = currentRoom.getRooms();
			Iterator<String> iter = roomNames.iterator();
			while (iter.hasNext()) {
				String room = iter.next();
				if (room != null && !room.equalsIgnoreCase(currentRoom.getName())) {
					if (!sendRoom(room)) {
						// if an error occurs stop spamming
						break;
					}
				}
			}
			break;
		case JOIN_ROOM:
			currentRoom.joinRoom(p.getMessage(), this);
			break;
		default:
			log.log(Level.INFO, "Unhandled payload on server: " + p);
			break;
		}
	}

	@Override
	public void run() {
		try {
			isRunning = true;
			Payload fromClient;
			while (isRunning && // flag to let us easily control the loop
					!client.isClosed() // breaks the loop if our connection closes
					&& (fromClient = (Payload) in.readObject()) != null // reads an object from inputStream (null would
			// likely mean a disconnect)
			) {
				System.out.println("Received from client: " + fromClient);
				processPayload(fromClient);
			} // close while loop
		} catch (Exception e) {
			// happens when client disconnects
			e.printStackTrace();
			log.log(Level.INFO, "Client Disconnected");
		} finally {
			isRunning = false;
			log.log(Level.INFO, "Cleaning up connection for ServerThread");
			cleanup();
		}
	}

	private void cleanup() {
		if (currentRoom != null) {
			log.log(Level.INFO, getName() + " removing self from room " + currentRoom.getName());
			currentRoom.removeClient(this);
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				log.log(Level.INFO, "Input already closed");
			}
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				log.log(Level.INFO, "Client already closed");
			}
		}
		if (client != null && !client.isClosed()) {
			try {
				client.shutdownInput();
			} catch (IOException e) {
				log.log(Level.INFO, "Socket/Input already closed");
			}
			try {
				client.shutdownOutput();
			} catch (IOException e) {
				log.log(Level.INFO, "Socket/Output already closed");
			}
			try {
				client.close();
			} catch (IOException e) {
				log.log(Level.INFO, "Client already closed");
			}
		}
	}
}