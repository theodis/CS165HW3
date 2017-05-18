package a3;

import graphicslib3D.*;
import sage.renderer.IRenderer;
import sage.camera.ICamera;
import sage.camera.JOGLCamera;
import sage.camera.controllers.AbstractCameraController;
import sage.input.IInputManager;
import sage.scene.SceneNode;
import sage.scene.shape.Pyramid;
import sage.scene.HUDString;
import sage.input.action.AbstractInputAction;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Event;
import myGameEngine.*;
import java.util.*;
import java.awt.Color;
import sage.terrain.*;

public class Player {
	private int score;

	private Tank playerNode;
	private ICamera camera;
	private OrbitCameraController cameraController;
	private HUDString bearingString;
	private HUDString pitchString;
	private HUDString powerString;
	private HUDString scoreString;
	private HUDString yourTurnString;
	private InputController inputController;

	public Color getTankColor() {
		return JSVars.getInst().getTankColor();
	}

	public void addPoint() {score++;}

	public boolean isDead() { return getSceneNode().isDead(); }

	public float deadZone(float val) {
		if(Math.abs(val) < 0.15) return 0.0f;
		return val;
	}

	public Player() {

		IRenderer renderer = Starter.getInst().getDisplaySystem().getRenderer();
		IInputManager input = Starter.getInst().getInputManager();
		
		//playerNode = new Pyramid();
		
		playerNode = new Tank();
		Matrix3D pnp = new Matrix3D();
		pnp.translate(Math.random() * 128,1, Math.random() * 128);
		playerNode.setLocalTranslation(pnp);
		playerNode.setColor(getTankColor());
		//playerNode.setColor(Color.BLUE.darker());

		//Set up camera and camera controller
		camera = new JOGLCamera(renderer);
		cameraController = new OrbitCameraController();
		cameraController.setCamera(camera);
		cameraController.setTarget(playerNode);

		//Set up input controller
		inputController = new InputController(Starter.getInst().getInputManager(), camera, 50);
		//inputController.addControlledNode(playerNode);
		
		//Set up HUD strings
		bearingString = new HUDString ("Bearing = " + Math.round(playerNode.getTopRotation() * 10) / 10);
		pitchString = new HUDString ("Pitch = " + Math.round(playerNode.getTurretPitch() * 10) / 10);
		powerString = new HUDString ("Power = " + Math.round(playerNode.getShootPower() * 10) / 10);
		scoreString = new HUDString ("Score = " + score);
		yourTurnString = new HUDString("");

		bearingString.setLocation(0.01,0.16);
		pitchString.setLocation(0.01,0.11);
		powerString.setLocation(0.01,0.06);
		scoreString.setLocation(0.01,0.01);
		yourTurnString.setLocation(0.45, 0.9);

		camera.addToHUD(bearingString);
		camera.addToHUD(pitchString);
		camera.addToHUD(powerString);
		camera.addToHUD(scoreString);
		camera.addToHUD(yourTurnString);

		//Bind movement inputs
		for(net.java.games.input.Controller c : input.getControllers()){
			if(c.getType() == net.java.games.input.Controller.Type.GAMEPAD)
				inputController.bindToController(c);
			if(c.getType() == net.java.games.input.Controller.Type.KEYBOARD)
				inputController.bindToController(c);
		}

		//Bind camera inputs
		for(net.java.games.input.Controller c : input.getControllers()){
			if(c.getType() == net.java.games.input.Controller.Type.GAMEPAD){
				//RX and RY work in both windows and linux
				input.associateAction(c, Identifier.Axis.RX, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Rotate(-1 * deadZone(event.getValue()) * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.RY, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Pitch(deadZone(event.getValue()) * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				
				//Handle windows mappings
				if(OSValidator.isWindows()){
					input.associateAction(c, Identifier.Button._0, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							cameraController.adjustRadius(time / 40);
						}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					input.associateAction(c, Identifier.Button._1, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							cameraController.adjustRadius(time / -40);
						}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				}
				
				//Handle linux mappings
				if(OSValidator.isUnix()){
					input.associateAction(c, Identifier.Button.A, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							cameraController.adjustRadius(time / 40);
						}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					input.associateAction(c, Identifier.Button.B, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							cameraController.adjustRadius(time / -40);
						}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				}
			}
/*			if(c.getType() == net.java.games.input.Controller.Type.MOUSE){
				input.associateAction(c, Identifier.Axis.X, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Rotate(-1 * deadZone(event.getValue()) * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Y, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Pitch(deadZone(event.getValue()) * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Z, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(deadZone(event.getValue()) * time / 10);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

			}*/
		}

		//Bind turret controls
		for(net.java.games.input.Controller c : input.getControllers()){
			if(c.getType() == net.java.games.input.Controller.Type.GAMEPAD){
				//X and Y axis thankfully work the same across platforms
				input.associateAction(c, Identifier.Axis.X, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						playerNode.rotateTop(-1 * deadZone(event.getValue()) * time / 10);
						Starter.getInst().renewPrediction();
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Y, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						playerNode.pitchTurret(deadZone(event.getValue()) * -time / 10);
						Starter.getInst().renewPrediction();
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

				//Handle linux mappings
				if(OSValidator.isUnix()){
					input.associateAction(c, Identifier.Axis.Z, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							playerNode.adjustPower( (deadZone(event.getValue()) + 1.0f) * time / 250);
							Starter.getInst().renewPrediction();
						}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					input.associateAction(c, Identifier.Axis.RZ, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							playerNode.adjustPower( -1 * (deadZone(event.getValue()) + 1.0f) * time / 250);
							Starter.getInst().renewPrediction();
						}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					input.associateAction(c, Identifier.Button.X, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							playerNode.fire();
						}}, IInputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
					input.associateAction(c, Identifier.Button.SELECT, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							Starter.getInst().setGameOver(true);
						}}, IInputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);				}
				
				//Handle windows mappings
				if(OSValidator.isWindows()){
					//Z axis in windows is for both triggers
					//Left trigger ranges 0 to 1
					//Right trigger ranges 0 to -1
					input.associateAction(c, Identifier.Axis.Z, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							playerNode.adjustPower( deadZone(event.getValue()) * time / 250);
							Starter.getInst().renewPrediction();
						}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					input.associateAction(c, Identifier.Button._2, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							playerNode.fire();
						}}, IInputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
					input.associateAction(c, Identifier.Button._6, 
						new AbstractInputAction(){public void performAction(float time, Event event) {
							Starter.getInst().setGameOver(true);
						}}, IInputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
				}
			}
			/*if(c.getType() == net.java.games.input.Controller.Type.MOUSE){
				input.associateAction(c, Identifier.Axis.X, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Rotate(-1 * deadZone(event.getValue()) * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Y, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Pitch(deadZone(event.getValue()) * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Z, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(deadZone(event.getValue()) * time / 10);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

			}*/
		}

		updateViewPort();

	}

	public void updateViewPort() {
		int width = Starter.getInst().getDisplaySystem().getWidth();
		int height = Starter.getInst().getDisplaySystem().getHeight();

		camera.setPerspectiveFrustum(60, (double)width / (double)height, 1.0, 1000.0);
	}

	public void updateHeight() {
		//Update elevation
		Matrix3D mat = playerNode.getLocalTranslation();
		Vector3D vec = mat.getCol(3);
		mat.translate(0,-vec.getY(),0);

		TerrainBlock tb = Starter.getInst().getTerrain();
		int dim = tb.getSize();
		float x = (float)vec.getX();
		float z = (float)vec.getZ();
		mat.translate(0, Starter.getInst().getHeightAt(x,z), 0);
		playerNode.setLocalTranslation(mat);

	}

	public void update(float elapsedTime) {
		bearingString.setText("Bearing = " + (Math.round(playerNode.getTopRotation() * 10) / 10.0));
		pitchString.setText("Pitch = " + (Math.round(playerNode.getTurretPitch() * 10) / 10.0));
		powerString.setText("Power = " + (Math.round(playerNode.getShootPower() * 10) / 10.0));
		scoreString.setText("Score = " + score);
		inputController.update(elapsedTime);
		updateHeight();
		cameraController.update(elapsedTime);

		if(getSceneNode().isTurn()){
			yourTurnString.setText("It's your turn!");
		} else {
			yourTurnString.setText("");
		}
	}

	public ICamera getCamera() {
		return camera;
	}

	public Tank getSceneNode() {
		return playerNode;
	}

	public Vector3D getPosition() {
		return playerNode.getWorldTranslation().getCol(3);
	}

}
