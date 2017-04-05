package a3;

import graphicslib3D.*;
import java.awt.Color;
import net.java.games.input.*;
import net.java.games.input.Event;
import sage.app.BaseGame;
import sage.camera.*;
import sage.display.*;
import sage.input.*;
import sage.scene.*;
import sage.event.*;
import java.io.IOException;
import sage.input.action.AbstractInputAction;
import sage.scene.shape.*;
import net.java.games.input.Component.Identifier;
import java.util.*;
import java.awt.event.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import myGameEngine.*;
import sage.renderer.*;
import sage.terrain.*;
import sage.scene.state.TextureState;
import sage.texture.Texture;
import sage.texture.TextureManager;
import sage.scene.state.RenderState;
import java.net.InetAddress;

public class Starter extends BaseGame {

	public enum CameraAction {
		MOVE_FORWARD,
		MOVE_BACKWARDS,
		MOVE_UP,
		MOVE_DOWN,
		MOVE_LEFT,
		MOVE_RIGHT,
		ROTATE_LEFT,
		ROTATE_RIGHT,
		PITCH_UP,
		PITCH_DOWN,
		ROLL_CLOCKWISE,
		ROLL_COUNTER_CLOCKWISE

	}

	private static String[] args;
	private static Starter inst;

	public static Starter getInst() {
		if(inst == null) inst = new Starter();
		return inst;
	}

	private Starter() {}

	private IDisplaySystem display;
	private IInputManager input;
	private IEventManager event;
	
	private HashMap<Identifier, AbstractInputAction> kbInputActions;

	private double time;

	private Random rand;

	private Player player;
	private HillHeightMap hills;
	private TerrainBlock terrain;
	private SkyBox sky;
	private GameClient client;

	public HillHeightMap getHills() { return hills; }
	public TerrainBlock getTerrain() { return terrain; }
	public Player getPlayer() { return player; }

	private ArrayList<String> getInputByType(net.java.games.input.Controller.Type t){
		ArrayList<String> ret = new ArrayList<String>();

		for(net.java.games.input.Controller c : input.getControllers())
			if(c.getType() == t)
				ret.add(c.getName());

		return ret;
	}

	public void removeObject(SceneNode sn) {
		removeGameWorldObject(sn);
	}

	public void addObject(SceneNode sn) {
		addGameWorldObject(sn);
	}

	protected void createDisplay() {
		IDisplaySystem display = (IDisplaySystem)new MyDisplaySystem(640,480,24,60,false,"sage.renderer.jogl.JOGLRenderer");
		System.out.print("\nWaiting for display creation...");
		int count = 0;

		while(!display.isCreated()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException("Display creation interrupted");
			}

			count++;
			System.out.print("+");
			if(count % 80 == 0) System.out.println();

			if(count > 2000) throw new RuntimeException("Unable to create display");
		}
		System.out.println();
		setDisplaySystem(display);
	}

	public void initGame() {

		time = 0;

		display = getDisplaySystem();
		display.setTitle("Assignment #3");

		rand = new Random();
		client = null;

		try {
			client = new GameClient(InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
			client.sendJoinMessage();
		} catch(IOException e) {
			e.printStackTrace();
		}

		//Axis set up
		Point3D origin = new Point3D(0,0,0);
		Point3D xEnd = new Point3D(1000,0,0);
		Point3D yEnd = new Point3D(0,1000,0);
		Point3D zEnd = new Point3D(0,0,1000);
		Line xAxis = new Line(origin,xEnd, Color.red, 2);
		Line yAxis = new Line(origin,yEnd, Color.green, 2);
		Line zAxis = new Line(origin,zEnd, Color.blue, 2);
		addGameWorldObject(xAxis);
		addGameWorldObject(yAxis);
		addGameWorldObject(zAxis);

		//Setup input
		kbInputActions = new HashMap<Identifier, AbstractInputAction>();
		kbInputActions.put(	Identifier.Key.ESCAPE,
							new AbstractInputAction(){public void performAction(float time, Event event) { 
								setGameOver(true); }});

		input = getInputManager();

		for(net.java.games.input.Controller c : input.getControllers()){
			System.out.print(c.getName());
			System.out.print(" : ");
			System.out.println(c.getType());
		}

		for(String kbName : getInputByType(net.java.games.input.Controller.Type.KEYBOARD))
			for(Identifier i : kbInputActions.keySet())
				input.associateAction(kbName, i, kbInputActions.get(i), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		//Setup event manager
		event = EventManager.getInstance();

		//Setup player
		player = new Player();
		display.getRenderer().setCamera(player.getCamera());

		addGameWorldObject(player.getSceneNode());

		//Setup hills
		hills = new HillHeightMap(129,2000,5.0f,20.0f,(byte)2,12345);
		hills.setHeightScale(0.1f);
		terrain = createTerBlock(hills);

		//Hills texture stuff
		TextureState grassState;
		Texture grassTexture = TextureManager.loadTexture2D("grass.png");
		grassTexture.setApplyMode(sage.texture.Texture.ApplyMode.Replace);
		grassState = (TextureState)display.getRenderer().createRenderState(RenderState.RenderStateType.Texture);
		grassState.setTexture(grassTexture,0);
		grassState.setEnabled(true);

		terrain.setRenderState(grassState);

		addGameWorldObject(terrain);

		//Skybox
		sky = new SkyBox();
		Texture sunTexture = TextureManager.loadTexture2D("sun.png");
		Texture skyTexture = TextureManager.loadTexture2D("blue.png");
		sky.setTexture(SkyBox.Face.Up, skyTexture);
		sky.setTexture(SkyBox.Face.North, skyTexture);
		sky.setTexture(SkyBox.Face.South, skyTexture);
		sky.setTexture(SkyBox.Face.East, sunTexture);
		sky.setTexture(SkyBox.Face.West, skyTexture);
		sky.scale(60,60,60);
		sky.setZBufferStateEnabled(false);
		addGameWorldObject(sky);

		super.update(0.0f);
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

	public void update(float elapsedTime) {

		if(client != null)
			client.update(elapsedTime);
		player.update(elapsedTime);
		if(sky != null && player != null && player.getSceneNode() != null)
			sky.setLocalTranslation(player.getSceneNode().getLocalTranslation());

		time += elapsedTime;

		super.update(elapsedTime);
	}

	protected void shutdown() {
		display.close();
	}

	public static void main (String[] args) {
		Starter.args = args;
		getInst().start();
	}

}
