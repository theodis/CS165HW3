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
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import sage.model.loader.OBJLoader;
import sage.audio.*;
import com.jogamp.openal.ALFactory;

public class Starter extends BaseGame implements IEventListener {

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
	private JSHeightMap jshm;
	private TerrainBlock terrain;
	private SkyBox sky;
	private GameClient client;
	private ArrayList<Explosion> explosions;
	private ArrayList<Explosion> removeExplosions;
	private ArrayList<Bomb> bombs;
	private ArrayList<Bomb> removeBombs;
	private ArrayList<Line> firePredict;
	private ArrayList<Line> firePredictShadow;
	private Texture treeTexture;
	private Texture houseTexture;
	private IAudioManager audioManager;
	private AudioResource shotSound, boomSound;
	private ArrayList<Sound> runningSounds;

	private int mapSeed;
	private int score;

	public int getMapSeed() { return mapSeed; }
	public HillHeightMap getHills() { return hills; }
	public TerrainBlock getTerrain() { return terrain; }
	public Player getPlayer() { return player; }
	public GameClient getClient() { return client; }

	public IEventManager getEventManager() { return event; }

	public boolean handleEvent(IGameEvent event) {

		if(event instanceof ExplosionEvent) {
			ExplosionEvent ee = (ExplosionEvent)event;
			Vector3D ePos = ee.getOrigin();
			Explosion e = new Explosion(ePos, ee.getRadius());
			explosions.add(e);
			addGameWorldObject(e);
			playBoom(new Point3D(ePos));
			client.sendExplodeMessage(ePos);
			return true;
		}
		if(event instanceof TankDestroyedEvent) {
			TankDestroyedEvent tde = (TankDestroyedEvent)event;
			Tank source = tde.getSource();
			Tank destroyed = tde.getDestroyed();
			client.sendDeadMessage(destroyed);

			//Award points if source is the players tank and
			//the destroyed tank isn't also the player
			if(source != destroyed && player.getSceneNode() == source){
				player.addPoint();
			}
			return true;
		}
		if(event instanceof Explosion.RemoveExplosionEvent) {
			Explosion.RemoveExplosionEvent ee = (Explosion.RemoveExplosionEvent)event;
			removeExplosions.add(ee.getSource());
			removeGameWorldObject(ee.getSource());
			return true;
		}
		return false;
	}

	public ArrayList<Bomb> getBombs() { return bombs; }

	public void addBomb(Tank s, Vector3D pos, Vector3D vel) {
		playShot(new Point3D(pos));
		Bomb b = new Bomb(s,pos,vel);
		bombs.add(b);
		addGameWorldObject(b);
	}

	public void removeBomb(Bomb b) {
		removeBombs.add(b);
		removeGameWorldObject(b);
	}

	private ArrayList<String> getInputByType(net.java.games.input.Controller.Type t){
		ArrayList<String> ret = new ArrayList<String>();

		for(net.java.games.input.Controller c : input.getControllers())
			if(c.getType() == t)
				ret.add(c.getName());

		return ret;
	}

	public void setupTrees() {

		//Add trees
		treeTexture = TextureManager.loadTexture2D("tree.png");
		for(int i = 0; i < 10; i++) 
			addRandomTree();

	}

	private void addRandomTree() {

		OBJLoader loader = new OBJLoader();
		TriMesh tree = loader.loadModel("tree.obj");
		tree.setTexture(treeTexture);
		float s = (float)(Math.random() * 0.1 + 0.2);
		float x = (float)(Math.random() * 128);
		float z = (float)(Math.random() * 128);

		Matrix3D scale = tree.getLocalScale();
		Matrix3D mat = new Matrix3D();
		TerrainBlock tb = getTerrain();
		int dim = tb.getSize();
		if(x >= 0 && x < dim - 1 && z >= 0 && z < dim - 1){
			mat.translate(x,
				tb.getHeight(x,z) - 1.5,
				z);
		}
		tree.setLocalTranslation(mat);
		scale.scale(s,s,s);

		addGameWorldObject(tree);

	}

	public void setupHouses() {

		//Add trees
		houseTexture = TextureManager.loadTexture2D("house.png");
		for(int i = 0; i < 10; i++) 
			addRandomHouse();

	}

	private void addRandomHouse() {

		OBJLoader loader = new OBJLoader();
		TriMesh tree = loader.loadModel("house.obj");
		tree.setTexture(houseTexture);
		float s = 1.25f;
		float x = (float)(Math.random() * 128);
		float z = (float)(Math.random() * 128);
		float r = (float)(Math.random() * 360);

		Matrix3D scale = tree.getLocalScale();
		Matrix3D mat = new Matrix3D();
		Matrix3D rot = tree.getLocalRotation();
		TerrainBlock tb = getTerrain();
		int dim = tb.getSize();
		if(x >= 0 && x < dim - 1 && z >= 0 && z < dim - 1){
			mat.translate(x,
				tb.getHeight(x,z) - 1.5,
				z);
		}
		tree.setLocalTranslation(mat);
		scale.scale(s,s,s);
		rot.rotateY(r);

		addGameWorldObject(tree);

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
		score = 0;
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
		//Point3D origin = new Point3D(0,0,0);
		//Point3D xEnd = new Point3D(1000,0,0);
		//Point3D yEnd = new Point3D(0,1000,0);
		//Point3D zEnd = new Point3D(0,0,1000);
		//Line xAxis = new Line(origin,xEnd, Color.red, 2);
		//Line yAxis = new Line(origin,yEnd, Color.green, 2);
		//Line zAxis = new Line(origin,zEnd, Color.blue, 2);
		//addGameWorldObject(xAxis);
		//addGameWorldObject(yAxis);
		//addGameWorldObject(zAxis);

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

		//Listen for explosions and tank destroyed
		event.addListener(this, ExplosionEvent.class);
		event.addListener(this, TankDestroyedEvent.class);
		event.addListener(this, Explosion.RemoveExplosionEvent.class);

		//Setup player
		player = new Player();
		display.getRenderer().setCamera(player.getCamera());

		addGameWorldObject(player.getSceneNode());

		setupTerrain(rand.nextInt());

		//Skybox
		sky = new SkyBox();
		Texture sunTexture = TextureManager.loadTexture2D("sun.png");
		Texture skyTexture = TextureManager.loadTexture2D("blue.png");
		Texture grassTexture = TextureManager.loadTexture2D("grass.png");
		sky.setTexture(SkyBox.Face.Up, skyTexture);
		sky.setTexture(SkyBox.Face.Down, grassTexture);
		sky.setTexture(SkyBox.Face.North, skyTexture);
		sky.setTexture(SkyBox.Face.South, skyTexture);
		sky.setTexture(SkyBox.Face.East, sunTexture);
		sky.setTexture(SkyBox.Face.West, skyTexture);
		sky.scale(500,500,500);
		sky.setZBufferStateEnabled(false);
		addGameWorldObject(sky);

		//Setup bomb stuff
		bombs = new ArrayList<Bomb>();
		removeBombs = new ArrayList<Bomb>();
		explosions = new ArrayList<Explosion>();
		removeExplosions = new ArrayList<Explosion>();

		//Audio initialization
		runningSounds = new ArrayList<Sound>();
		audioManager = AudioManagerFactory.createAudioManager("sage.audio.joal.JOALAudioManager");
		if(!audioManager.initialize())
			System.out.println("Failed to initialize audio");
		shotSound = audioManager.createAudioResource("shot.wav", AudioResourceType.AUDIO_SAMPLE);
		boomSound = audioManager.createAudioResource("boom.wav", AudioResourceType.AUDIO_SAMPLE);
		audioManager.getEar().setLocation(player.getCamera().getLocation());
		audioManager.getEar().setOrientation(player.getCamera().getViewDirection(), new Vector3D(0,1,0));
		updateEar();

		super.update(0.0f);
	}

	private void updateEar(){
		audioManager.getEar().setLocation(player.getCamera().getLocation());
		audioManager.getEar().setOrientation(player.getCamera().getViewDirection(), new Vector3D(0,1,0));
	}

	
	private Sound makeSound(AudioResource r, Point3D pos, int volume){
		Sound ret = new Sound(r, SoundType.SOUND_EFFECT, volume, false);
		ret.initialize(audioManager);
		ret.setLocation(pos);
		ret.setMaxDistance(1000.0f);
		ret.setMinDistance(10.0f);
		ret.setRollOff(2.0f);
		return ret;
	}
	private void playShot(Point3D loc){
		Sound s = makeSound(shotSound, loc, 100);
		runningSounds.add(s);
		s.play();
	}

	private void playBoom(Point3D loc){
		Sound s = makeSound(boomSound, loc, 100);
		runningSounds.add(s);
		s.play();

	}

	public void setupTerrain(int seed) {

		mapSeed = seed;
		if(terrain != null)
			removeGameWorldObject(terrain);

		//Setup hills
		hills = new HillHeightMap(129,2000,5.0f,20.0f,(byte)2,seed);
		hills.setHeightScale(0.1f);

		//jshm = new JSHeightMap("dostuff.js","a");

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

	public void clearPrediction() {
		for(Line l : firePredict)
			removeGameWorldObject(l);
		firePredict.clear();
		for(Line l : firePredictShadow)
			removeGameWorldObject(l);
		firePredictShadow.clear();
		
		firePredict = null;
		firePredictShadow = null;
	}
	
	public void renewPrediction() {
		if(firePredict != null) clearPrediction(); // Clear old prediction if it exists
		if(player.isDead()) return; //Don't bother with predictions for a dead player
		firePredict = new ArrayList<Line>();
		firePredictShadow = new ArrayList<Line>();
		
		//Show fire prediction
		Point3D[] points = getPlayer().getSceneNode().predictPath((int)(getPlayer().getSceneNode().getShootPower() / 12) + 10 ,100);
		for(int i = 0; i < points.length - 1; i++){
			Point3D shadowA = new Point3D(
				points[i].getX(),
				getHeightAt((float)points[i].getX(), (float)points[i].getZ()),
				points[i].getZ());
			Point3D shadowB = new Point3D(
				points[i + 1].getX(),
				getHeightAt((float)points[i + 1].getX(), (float)points[i + 1].getZ()),
				points[i + 1].getZ());

			firePredict.add(new Line(points[i], points[i + 1],  Color.RED, 1));
			firePredictShadow.add(new Line(shadowA, shadowB,  Color.BLACK, 1));
		}

		for(Line l : firePredict)
			addGameWorldObject(l);
		for(Line l : firePredictShadow)
			addGameWorldObject(l);
	}
	
	public void update(float elapsedTime) {

		if(client != null)
			client.update(elapsedTime);
		player.update(elapsedTime);
		if(sky != null && player != null && player.getSceneNode() != null) {
			Point3D loc = player.getCamera().getLocation();
			Matrix3D mat = new Matrix3D();
			mat.translate(loc.getX(), 490, loc.getZ());
			sky.setLocalTranslation(mat);
		}

		for(Bomb b : removeBombs){
			bombs.remove(b);
			Tank source = b.getSource();
			Vector3D origin = b.getWorldTranslation().getCol(3);
			float radius = 5.0f;
			event.triggerEvent(new ExplosionEvent(source,origin,radius));
		}
		removeBombs.clear();

		for(Explosion e : removeExplosions)
			explosions.remove(e);
		removeExplosions.clear();
		for(Explosion e : explosions)
			e.update(elapsedTime);
		for(Bomb b : bombs)
			b.update(elapsedTime);

		time += elapsedTime;

		updateEar();

		super.update(elapsedTime);
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
	
	protected void shutdown() {
		display.close();
	}

	public static void main (String[] args) {
		Starter.args = args;
		getInst().start();
	}

}
