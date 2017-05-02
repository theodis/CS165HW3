import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.*;
import java.lang.Thread;

import sage.networking.server.GameConnectionServer;
import sage.networking.server.IClientInfo;

import graphicslib3D.*;

public class GameServer extends GameConnectionServer<UUID> {

	public class AITank {
		private UUID id;
		private float bearing;
		private float pitch;
		private float x;
		private float y;
		private float z;

		private float lastBearing;
		private float lastPitch;

		private float hitX;
		private float hitY;
		private float hitZ;

		public AITank(GameServer g){
			id = UUID.randomUUID();
			x = (float)Math.random() * 128;
			y = 0; //FIXME: Pull from heightmap
			z = (float)Math.random() * 128;

			lastBearing = lastPitch = -1;
		}
	}

	private int hillsSeed;
	private HashMap<UUID, Long> timeSincePing;
	private HashMap<UUID, Point3D> positions;
	private ArrayList<AITank> aitanks;

	private static long getTime() {
		Date d = new Date();
		return d.getTime();
	}

	public static void main(String[] args) throws IOException {
		(new GameServer(Integer.parseInt(args[0]))).loop();
	}

	public void resetGame() {
		System.out.println("Resetting game");
	}

	public void firstPlayer(UUID clientID) {
		System.out.println("First player joined");
		aitanks = new ArrayList<AITank>();
		aitanks.add(new AITank(this));
		aitanks.add(new AITank(this));
	}

	public void loop() {

		resetGame();

		while(true) {

			ArrayList<UUID> remove = new ArrayList<UUID>();
			long curTime = getTime();
			for(UUID clientID : timeSincePing.keySet())
				if(getTime() - timeSincePing.get(clientID) > 3000)
					remove.add(clientID);

			//Drop clients that haven't sent a ping in 5 seconds
			for(UUID clientID : remove) {
				sendByeMessages(clientID);
				removeClient(clientID);
				timeSincePing.remove(clientID);
				System.out.println("Removed client " + clientID + " due to inactivity.");
			}

			if(timeSincePing.size() == 0 && remove.size() > 0) {
				resetGame();
			}
			try{
				Thread.sleep(100);
			} catch(Exception e) {
				break;
			}
		}
	}

	public GameServer(int localPort) throws IOException{
		super(localPort, ProtocolType.TCP);
		timeSincePing = new HashMap<UUID, Long>();
	}

	public void acceptClient(IClientInfo ci, Object o) {
		String message = (String)o;
		System.out.println(message);
		String[] messageTokens = message.split(",");

		if(messageTokens.length > 0) {
			if(messageTokens[0].compareTo("join") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				addClient(ci, clientID);
				timeSincePing.put(clientID, getTime());
				sendJoinedMessage(clientID, true);
				if(timeSincePing.size() == 1)
					firstPlayer(clientID);
			}
		}
	}

	public void processPacket(Object o, InetAddress senderIP, int sndPort) {
		String message = (String) o;
		String[] msgTokens = message.split(",");
		System.out.println(message);

		if(msgTokens.length > 0) {
			if(msgTokens[0].compareTo("bye") == 0) {
				//format: bye,localid
				UUID clientID = UUID.fromString(msgTokens[1]);
				sendByeMessages(clientID);
				removeClient(clientID);
			}

			if(msgTokens[0].compareTo("hi") == 0) {
				//format: hi,localid,destid,x,y,z
				UUID localID = UUID.fromString(msgTokens[1]);
				UUID destinationID = UUID.fromString(msgTokens[2]);
				String[] pos = {msgTokens[3], msgTokens[4], msgTokens[5], msgTokens[6], msgTokens[7]};
				sendHiMessages(localID,destinationID, pos);
			}

			if(msgTokens[0].compareTo("create") == 0) {
				//format: create,localid,x,y,z
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4], msgTokens[5], msgTokens[6]};
				sendCreateMessages(clientID, pos);
			}

			if(msgTokens[0].compareTo("move") == 0) {
				//format: move,localid,x,y,z
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4], msgTokens[5], msgTokens[6]};
				sendMoveMessages(clientID, pos);
			}

			if(msgTokens[0].compareTo("fire") == 0) {
				//format: move,localid,x,y,z
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4], msgTokens[5], msgTokens[6], msgTokens[7]};
				sendFireMessages(clientID, pos);
			}

			if(msgTokens[0].compareTo("ping") == 0) {
				//format: ping,localid,x
				UUID clientID = UUID.fromString(msgTokens[1]);
				timeSincePing.put(clientID, getTime());
			}
		}
	}

	public void sendJoinedMessage(UUID clientID, boolean success) {
		try {
			String message = new String("join,");
			if(success) message += "success";
			else message += "failure";
			System.out.println(message);
			sendPacket(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendHiMessages(UUID localID, UUID destID, String[] position) {
		try {
			String message = new String("hi," + localID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + position[3];
			message += "," + position[4];
			System.out.println(message);
			sendPacket(message, destID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendCreateMessages(UUID clientID, String[] position) {
		try {
			String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + position[3];
			message += "," + position[4];
			System.out.println(message);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMoveMessages(UUID clientID, String[] position) {
		try {
			String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + position[3];
			message += "," + position[4];
			System.out.println(message);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendFireMessages(UUID clientID, String[] position) {
		try {
			String message = new String("fire," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + position[3];
			message += "," + position[4];
			message += "," + position[5];
			System.out.println(message);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendByeMessages(UUID clientID) {
		try {
			String message = new String("bye," + clientID.toString());
			System.out.println(message);
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
