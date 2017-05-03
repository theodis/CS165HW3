import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.*;
import java.lang.Thread;

import sage.networking.server.GameConnectionServer;
import sage.networking.server.IClientInfo;
import sage.terrain.*;

import graphicslib3D.*;

public class GameServer extends GameConnectionServer<UUID> {

	public class AITank {
		private GameServer server;

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

		public String[] getPosStrings() {
			return new String[] {
				((Float)x).toString(),
				((Float)y).toString(),
				((Float)z).toString(),
				((Float)bearing).toString(),
				((Float)pitch).toString(),
			};

		}

		public AITank(GameServer g){
			server = g;
			id = UUID.randomUUID();
			x = (float)Math.random() * 128;
			z = (float)Math.random() * 128;
			y = g.getHeightAt(x,z);

			lastBearing = lastPitch = 0;
			server.sendCreateMessages(id, getPosStrings());
		}

		public void sendMoveMessage() {
			String[] pos = new String[] {
				((Float)x).toString(),
				((Float)y).toString(),
				((Float)z).toString(),
				((Float)bearing).toString(),
				((Float)pitch).toString(),
			};
			server.sendMoveMessages(id, getPosStrings());
		}

		public void sendHiMessage(UUID dest) {
			server.sendHiMessages(id, dest, getPosStrings());
		}
	}

	private int mapSeed;
	private HashMap<UUID, Long> timeSincePing;
	private HashMap<UUID, Point3D> positions;
	private ArrayList<AITank> aitanks;

	private TerrainBlock terrain;
	public TerrainBlock getTerrain() {return terrain;}

    public void setupTerrain(int seed) {
		//Setup hills
        HillHeightMap hills = new HillHeightMap(129,2000,5.0f,20.0f,(byte)2,seed);
        hills.setHeightScale(0.1f);

        terrain = createTerBlock(hills);
	}

    private TerrainBlock createTerBlock(AbstractHeightMap heightMap) {
        float heightScale = 0.05f;
        Vector3D terrainScale = new Vector3D(1,heightScale,1);
        int terrainSize = heightMap.getSize();

        float cornerHeight = heightMap.getTrueHeightAtPoint(0,0) * heightScale;
        Point3D terrainOrigin = new Point3D(0, -cornerHeight, 0);
        String name = "Terrain:" + heightMap.getClass().getSimpleName();
        TerrainBlock ret = new TerrainBlock(name, terrainSize, terrainScale, heightMap.getHeightData(), terrainOrigin);
        return ret;
    }

    public float getHeightAt(float x, float z) {
        TerrainBlock tb = getTerrain();
        int dim = tb.getSize();
        float ret = 0;
        if(x >= 0 && x < dim - 1 && z >= 0 && z < dim - 1){
            ret = tb.getHeight(x,z) - 1.5f;
        }

        return ret;
    }

	private static long getTime() {
		Date d = new Date();
		return d.getTime();
	}

	public static void main(String[] args) throws IOException {
		(new GameServer(Integer.parseInt(args[0]))).loop();
	}

	public void resetGame() {
		System.out.println("Resetting game");
		/*Random r = new Random();
		mapSeed = r.nextInt();*/
		mapSeed = 12345;
		setupTerrain(mapSeed);
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
				for(AITank ai : aitanks){
					ai.sendHiMessage(clientID);
				}
				sendMapSeed();
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

	public void sendMapSeed() {
		try {
			String message = new String("map," + mapSeed);
			System.out.println(message);
			sendPacketToAll(message);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
