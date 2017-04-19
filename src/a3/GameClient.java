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
				sendCreateMessage(game.getPlayer().getPosition());
				timeSinceSend = 0;
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
			Ghost g = new Ghost(x,y,z);
			ghosts.put(ghostID, g);
			game.addObject(g.getNode());
			sendHiMessage(game.getPlayer().getPosition(), ghostID);
		}
		if(messageTokens[0].compareTo("hi") == 0) {
			UUID ghostID = UUID.fromString(messageTokens[1]);
			float x = Float.parseFloat(messageTokens[2]);
			float y = Float.parseFloat(messageTokens[3]);
			float z = Float.parseFloat(messageTokens[4]);
			Ghost g = new Ghost(x,y,z);
			ghosts.put(ghostID, g);
			game.addObject(g.getNode());
		}
		if(messageTokens[0].compareTo("move") == 0) {
			UUID ghostID = UUID.fromString(messageTokens[1]);
			float x = Float.parseFloat(messageTokens[2]);
			float y = Float.parseFloat(messageTokens[3]);
			float z = Float.parseFloat(messageTokens[4]);
			if(ghosts.containsKey(ghostID))
				ghosts.get(ghostID).move(x,y,z);
		}
	}

	public void sendCreateMessage(Vector3D pos) {
		try {
			String message = new String("create," + id.toString());
			message += "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();
			System.out.println(message);
			sendPacket(message);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void sendHiMessage(Vector3D pos, UUID destID) {
		try {
			String message = new String("hi," + id.toString() + "," + destID.toString());
			message += "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();
			System.out.println(message);
			sendPacket(message);
		} catch(IOException e) {
			e.printStackTrace();
		}

	}

	public void sendMoveMessage() {
		Vector3D pos = Starter.getInst().getPlayer().getPosition();
		try {
			String message = new String("move," + id.toString());
			message += "," + pos.getX() + "," + pos.getY() + "," + pos.getZ();
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

	public void update(float elapsed) {
		processPackets();
		if(connected){
			timeSinceSend += elapsed;
			if(timeSinceSend > 100) {
				sendMoveMessage();
				timeSinceSend = 0;
			}
		}
	}
}
