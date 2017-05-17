package a3;

import graphicslib3D.*;
import sage.networking.client.GameConnectionClient;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import sage.networking.IGameConnection.ProtocolType;

public class GameClient extends GameConnectionClient {
	private UUID id;
	private float timeSinceSend;
	private float timeSincePing;
	private HashMap<UUID, Ghost> ghosts;
	private boolean connected;
	private String oldMessage = "";

	public GameClient(InetAddress remAddr, int remPort) throws IOException {
		super(remAddr, remPort, ProtocolType.TCP);
		id = UUID.randomUUID();
		ghosts = new HashMap<UUID, Ghost>();
	}

	protected void processPacket(Object msg) {
		System.out.println(msg.toString());
		Starter game = Starter.getInst();
		String[] messageTokens = ((String) msg).split(",");
		if(messageTokens[0].compareTo("join")==0) {
			if(messageTokens[1].compareTo("success") == 0) {
				connected = true;
				sendCreateMessage();
				timeSinceSend = 0;
				timeSincePing = 0;
			} else {
				//Silently fail for now.  FIXME!
			}
		}

		if(messageTokens[0].compareTo("bye") == 0) {
			UUID ghostID = UUID.fromString(messageTokens[1]);
			game.removeObject(ghosts.get(ghostID).getNode());
			ghosts.remove(ghostID);
		}

		if(messageTokens[0].compareTo("create") == 0) {
			UUID ghostID = UUID.fromString(messageTokens[1]);
			float x = Float.parseFloat(messageTokens[2]);
			float y = Float.parseFloat(messageTokens[3]);
			float z = Float.parseFloat(messageTokens[4]);
			float top = Float.parseFloat(messageTokens[5]);
			float tur = Float.parseFloat(messageTokens[6]);
			Ghost g = new Ghost(x,y,z);
			Tank t = g.getNode();
			t.setTopRotation(top);
			t.setTurretPitch(tur);
			ghosts.put(ghostID, g);
			game.addObject(g.getNode());
			sendHiMessage(ghostID);
		}
		if(messageTokens[0].compareTo("hi") == 0) {
			UUID ghostID = UUID.fromString(messageTokens[1]);
			float x = Float.parseFloat(messageTokens[2]);
			float y = Float.parseFloat(messageTokens[3]);
			float z = Float.parseFloat(messageTokens[4]);
			float top = Float.parseFloat(messageTokens[5]);
			float tur = Float.parseFloat(messageTokens[6]);
			Ghost g = new Ghost(x,y,z);
			Tank t = g.getNode();
			t.setTopRotation(top);
			t.setTurretPitch(tur);
			ghosts.put(ghostID, g);
			game.addObject(g.getNode());
		}
		if(messageTokens[0].compareTo("move") == 0) {
			UUID ghostID = UUID.fromString(messageTokens[1]);
			float x = Float.parseFloat(messageTokens[2]);
			float y = Float.parseFloat(messageTokens[3]);
			float z = Float.parseFloat(messageTokens[4]);
			float top = Float.parseFloat(messageTokens[5]);
			float tur = Float.parseFloat(messageTokens[6]);
			if(ghosts.containsKey(ghostID)){
				Ghost g = ghosts.get(ghostID);
				g.move(x,y,z);
				Tank t = g.getNode();
				t.setTopRotation(top);
				t.setTurretPitch(tur);

			}
		}
		if(messageTokens[0].compareTo("fire") == 0) {
			UUID ghostID = UUID.fromString(messageTokens[1]);
			float px = Float.parseFloat(messageTokens[2]);
			float py = Float.parseFloat(messageTokens[3]);
			float pz = Float.parseFloat(messageTokens[4]);
			float vx = Float.parseFloat(messageTokens[5]);
			float vy = Float.parseFloat(messageTokens[6]);
			float vz = Float.parseFloat(messageTokens[7]);
			if(ghosts.containsKey(ghostID)){
				game.addBomb(ghosts.get(ghostID).getNode(), new Vector3D(px,py,pz), new Vector3D(vx,vy,vz));
			}
		}
		if(messageTokens[0].compareTo("map") == 0) {
			int newseed = Integer.parseInt(messageTokens[1]);
			Starter.getInst().setupTerrain(newseed);
			Starter.getInst().setupTrees();
		}
	}

	public String positionString() {
		Player p = Starter.getInst().getPlayer();
		Tank t = p.getSceneNode();
		Vector3D pos = p.getPosition();
		return pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + t.getTopRotation() + "," + t.getTurretPitch();
	}

	public void sendCreateMessage() {
		try {
			String message = new String("create," + id.toString());
			message += "," + positionString() + "," + Starter.getInst().getMapSeed();
			System.out.println(message);
			sendPacket(message);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void sendHiMessage(UUID destID) {
		try {
			String message = new String("hi," + id.toString() + "," + destID.toString());
			message += "," + positionString();
			System.out.println(message);
			sendPacket(message);
		} catch(IOException e) {
			e.printStackTrace();
		}

	}

	public void sendMoveMessage() {
		try {
			String message = new String("move," + id.toString());
			message += "," + positionString();
			if(!oldMessage.equals(message)){
				System.out.println(message);
				sendPacket(message);
				oldMessage = message;
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void sendJoinMessage() {
		try {
			String join = new String("join," + id.toString());
			System.out.println(join);
			sendPacket(join);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendFireMessage(Vector3D pos, Vector3D vel) {
		try {
			String message = new String("fire," + id.toString());
			message += "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();
			message += "," + vel.getX() + "," + vel.getY() + "," + vel.getZ();
			System.out.println(message);
			sendPacket(message);
		} catch(IOException e) {
			e.printStackTrace();
		}

	}

	public void sendPingMessage() {
		try {
			String message = new String("ping," + id.toString());
			System.out.println(message);
			sendPacket(message);
		} catch(IOException e) {
			e.printStackTrace();
		}


	}

	public void update(float elapsed) {
		processPackets();
		if(connected){
			timeSinceSend += elapsed;
			if(timeSinceSend > 100) {
				sendMoveMessage();
				timeSinceSend = 0;
			}

			//Ping every second
			timeSincePing += elapsed;
			if(timeSincePing > 1000) {
				sendPingMessage();
				timeSincePing = 0;
			}

		}
	}
}
