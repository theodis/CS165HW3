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
		private UUID target;

		private float bearing;
		private float pitch;
		private float power = 15.0f;
		private float x;
		private float y;
		private float z;

		private float lastBearing;
		private float lastPitch;

		private float hitX;
		private float hitY;
		private float hitZ;

		private int timeSinceTargetChange;
		private int timeSinceLastShot;
		private float desiredBearing;
		private float desiredPitch;

		public UUID getID() { return id; }

		public String[] getPosStrings() {
			return new String[] {
				((Float)x).toString(),
				((Float)y).toString(),
				((Float)z).toString(),
				((Float)bearing).toString(),
				((Float)pitch).toString(),
				"255","255","255"
			};

		}

		public void hitAt(Vector3D v) {
			//TODO adjust power
		}

		public AITank(GameServer g){
			server = g;
			id = UUID.randomUUID();
			x = (float)Math.random() * 128;
			z = (float)Math.random() * 128;
			y = g.getHeightAt(x,z);

			desiredBearing = -1;
			desiredPitch = -1;
			lastBearing = lastPitch = 0;
			//server.sendCreateMessages(id, getPosStrings());
			sendMoveMessage();
		}

		public void pickTarget() {
			Random r = new Random();
			HashMap<UUID, Point3D> pos = server.getPositions();
			if(pos.size() <= 1) return; //Don't try to pick a target if no one is around
			do{
				int targetInd = Math.abs(r.nextInt()) % pos.size();
				target = (UUID)pos.keySet().toArray()[targetInd];
			}while(id != target);
		}

		private Vector3D[] getFirePosAndVel() { 
			Vector3D[] ret = new Vector3D[2];
			Point3D p = server.getPositions().get(id);
			Vector3D pos = new Vector3D(p.getX(), p.getY(), p.getZ());
			double alpha = ((360 - bearing) * Math.PI) / 180;
			double beta = (pitch * Math.PI) / 180;
			double x = Math.cos(alpha) * Math.cos(beta);
			double z = Math.sin(alpha) * Math.cos(beta);
			double y = Math.sin(beta);
			Vector3D vel = new Vector3D(x,y,z);
			vel.scale(power);

			//Move the bomb forward a bit
			vel.scale(1/100.0);
			pos = pos.add(vel);
			vel.scale(100/1.0);

			ret[0] = pos;
			ret[1] = vel;

			return ret;
		}

		public void calculateBearing() {
			Point3D source = server.getPositions().get(id);
			Point3D dest = server.getPositions().get(target);

			if(source == null || dest == null) return;

			float dx = (float)(dest.getX() - source.getX());
			float dz = (float)(dest.getY() - source.getY());
			bearing = (float)(Math.atan2(dz,dx) * 180 / Math.PI);
			pitch = 45.0f;
			power = 15.0f;
		}

		public void update(int elapsedTime) {
			Random r = new Random();
			float deltaBearing = desiredBearing - bearing;
			float deltaPitch = desiredPitch - pitch;
			if(desiredBearing == -1 || timeSinceTargetChange > 30000) {
				pickTarget();
				calculateBearing();
				timeSinceTargetChange = -1 * (r.nextInt() % 3000);
			} else if(deltaBearing != 0 || deltaPitch != 0){
				//If not at desired pitch or bearing then move in that direction
				//Cap change to 10 per update
				if(Math.abs(deltaBearing) > 10) deltaBearing = deltaBearing * 10 / Math.abs(deltaBearing);
				if(Math.abs(deltaPitch) > 10) deltaPitch = deltaPitch * 10 / Math.abs(deltaPitch);
				bearing += deltaBearing;
				pitch += deltaPitch;
				sendMoveMessage();
			} else if(timeSinceLastShot > 5000) {
				System.out.println("Firing");
				sendFireMessage();
				timeSinceLastShot = -1 * (r.nextInt() % 2000);
			}
			timeSinceLastShot += elapsedTime;
			timeSinceTargetChange += elapsedTime;
		}

		public void fire() {
			sendFireMessage();
		}

		public void sendFireMessage() {
			Vector3D[] posVel = getFirePosAndVel();
			server.sendFireMessages(id, new String[]{
				((Double)posVel[0].getX()).toString(),	
				((Double)posVel[0].getY()).toString(),	
				((Double)posVel[0].getZ()).toString(),	
				((Double)posVel[1].getX()).toString(),	
				((Double)posVel[1].getY()).toString(),	
				((Double)posVel[1].getZ()).toString(),	
			});
		}

		public void sendMoveMessage() {

			setPosition(id, new Point3D(x,y,z));

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
	private HashMap<UUID, AITank> aitanks;

	private Vector<UUID> turnOrder;
	private UUID curPlayer;

	private HashMap<UUID, Vector3D> booms;

	private TerrainBlock terrain;
	public TerrainBlock getTerrain() {return terrain;}

	public HashMap<UUID, Point3D> getPositions() { return positions; }

	public void setPosition(UUID client, Point3D pos) {
		positions.put(client, pos);
	}

	public void removePosition(UUID client) {
		positions.remove(client);
	}

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
		aitanks = new HashMap<UUID, AITank>();
		addAITank();
		addAITank();
		addAITank();
		addAITank();
		addAITank();
		addAITank();
	}

	public void addAITank() {
		AITank t = new AITank(this);
		aitanks.put(t.getID(), t);
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
				dropPlayer(clientID);
				System.out.println("Removed client " + clientID + " due to inactivity.");
			}

			if(timeSincePing.size() == 0 && remove.size() > 0) {
				resetGame();
			}

			if(aitanks != null)
				for(AITank ai : aitanks.values())
					ai.update(100); // Just assume 100ms

			try{
				Thread.sleep(100);
			} catch(Exception e) {
				break;
			}
		}
	}

	public GameServer(int localPort) throws IOException{
		super(localPort, ProtocolType.TCP);
		positions = new HashMap<UUID, Point3D>();
		timeSincePing = new HashMap<UUID, Long>();
		turnOrder = new Vector<UUID>();
		booms = new HashMap<UUID, Vector3D>();
		curPlayer = null;
	}

	public void AddBoom(UUID source, Vector3D loc) {
		booms.put(source, loc);
		if(booms.keySet().size() >= timeSincePing.keySet().size()) nextTurn();
	}

	public void dropPlayer(UUID id) {
		sendByeMessages(id);
		timeSincePing.remove(id);
		removeClient(id);
		if(booms.containsKey(id)) booms.remove(id);
		if(booms.keySet().size() >= timeSincePing.keySet().size()) nextTurn();
		else if(curPlayer == id) nextTurn();
	}

	public void nextTurn() {
		if(curPlayer != null && aitanks.containsKey(curPlayer)) {
			//AI tank so calculate roughly where explosion happened.
			float x = 0;
			float y = 0;
			float z = 0;

			for(Vector3D v : booms.values()){
				x += v.getX();
				y += v.getY();
				z += v.getZ();
			}

			x /= booms.size();
			y /= booms.size();
			z /= booms.size();

			aitanks.get(curPlayer).hitAt(new Vector3D(x,y,z));
		}

		if(curPlayer == null)
			curPlayer = turnOrder.get(0);
		else{
			int ind = turnOrder.indexOf(curPlayer);
			ind++;
			ind %= turnOrder.size();
			curPlayer = turnOrder.get(ind);
		}

		sendTurnMessage(curPlayer);
		if(aitanks.containsKey(curPlayer))
			aitanks.get(curPlayer).fire();
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
				dropPlayer(clientID);
			}

			if(msgTokens[0].compareTo("hi") == 0) {
				//format: hi,localid,destid,x,y,z,rot,pitch,r,g,b
				UUID localID = UUID.fromString(msgTokens[1]);
				UUID destinationID = UUID.fromString(msgTokens[2]);
				String[] pos = {msgTokens[3], msgTokens[4], msgTokens[5], msgTokens[6], msgTokens[7], msgTokens[8], msgTokens[9], msgTokens[10]};
				sendHiMessages(localID,destinationID, pos);
			}

			if(msgTokens[0].compareTo("create") == 0) {
				//format: create,localid,x,y,z,rot,pitch,r,g,b
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4], msgTokens[5], msgTokens[6], msgTokens[7], msgTokens[8], msgTokens[9]};
				sendCreateMessages(clientID, pos);
				for(AITank ai : aitanks.values()){
					ai.sendHiMessage(clientID);
				}
				sendMapSeed();
			}

			if(msgTokens[0].compareTo("move") == 0) {
				//format: move,localid,x,y,z,rot,pitch,r,g,b
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4], msgTokens[5], msgTokens[6], msgTokens[7], msgTokens[8], msgTokens[9]};
				setPosition(clientID, new Point3D(
							Float.parseFloat(msgTokens[2]),
							Float.parseFloat(msgTokens[3]),
							Float.parseFloat(msgTokens[4])));
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

			if(msgTokens[0].compareTo("boom") == 0) {
				//format: ping,localid,x,y,z

				UUID clientID = UUID.fromString(msgTokens[2]);
				float x = Float.parseFloat(msgTokens[2]);
				float y = Float.parseFloat(msgTokens[3]);
				float z = Float.parseFloat(msgTokens[4]);
			}

			if(msgTokens[0].compareTo("dead") == 0) {
				//format: ping,localid,deadid
				UUID deadID = UUID.fromString(msgTokens[2]);
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
			message += "," + position[5];
			message += "," + position[6];
			message += "," + position[7];
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
			message += "," + position[5];
			message += "," + position[6];
			message += "," + position[7];
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
			message += "," + position[5];
			message += "," + position[6];
			message += "," + position[7];
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

	public void sendTurnMessage(UUID who) {
		try {
			String message = new String("turn," + who.toString());
			System.out.println(message);
			sendPacketToAll(message);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
