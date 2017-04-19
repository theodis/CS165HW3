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
	private HUDString scoreString;
	private InputController inputController;

	public Player() {

		IRenderer renderer = Starter.getInst().getDisplaySystem().getRenderer();
		IInputManager input = Starter.getInst().getInputManager();
		
		//playerNode = new Pyramid();
		
		playerNode = new Tank();
		Matrix3D pnp = new Matrix3D();
		pnp.translate(Math.random() * 128,1, Math.random() * 128);
		playerNode.setLocalTranslation(pnp);
		playerNode.setColor(Color.BLUE.darker());

		//Set up camera and camera controller
		camera = new JOGLCamera(renderer);
		cameraController = new OrbitCameraController();
		cameraController.setCamera(camera);
		cameraController.setTarget(playerNode);

		//Set up input controller
		//inputController = new InputController(Starter.getInst().getInputManager(), camera, 50);
		//inputController.addControlledNode(playerNode);
		
		//Set up HUD strings
		scoreString = new HUDString ("Score = " + score);

		camera.addToHUD(scoreString);

		//Bind movement inputs
		/*for(net.java.games.input.Controller c : input.getControllers()){
			if(c.getType() == net.java.games.input.Controller.Type.GAMEPAD)
				inputController.bindToController(c);
			if(c.getType() == net.java.games.input.Controller.Type.KEYBOARD)
				inputController.bindToController(c);
		}*/

		//Bind camera inputs
		for(net.java.games.input.Controller c : input.getControllers()){
			if(c.getType() == net.java.games.input.Controller.Type.GAMEPAD){
				input.associateAction(c, Identifier.Axis.RX, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Rotate(-1 * event.getValue() * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.RY, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Pitch(event.getValue() * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Button._1, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(time / 40);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Button._2, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(time / -40);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Button.A, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(time / 40);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Button.B, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(time / -40);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			}
			if(c.getType() == net.java.games.input.Controller.Type.MOUSE){
				input.associateAction(c, Identifier.Axis.X, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Rotate(-1 * event.getValue() * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Y, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Pitch(event.getValue() * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Z, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(event.getValue() * time / 10);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

			}
		}

		//Bind turret controls
		for(net.java.games.input.Controller c : input.getControllers()){
			if(c.getType() == net.java.games.input.Controller.Type.GAMEPAD){
				input.associateAction(c, Identifier.Axis.X, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						playerNode.rotateTop(-1 * event.getValue() * time / 10);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Y, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						playerNode.pitchTurret(event.getValue() * -time / 10);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				/*input.associateAction(c, Identifier.Button._1, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(time / 40);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Button._2, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(time / -40);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);*/
			}
			/*if(c.getType() == net.java.games.input.Controller.Type.MOUSE){
				input.associateAction(c, Identifier.Axis.X, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Rotate(-1 * event.getValue() * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Y, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.Pitch(event.getValue() * time / 1000);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.Z, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						cameraController.adjustRadius(event.getValue() * time / 10);
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

			}*/
		}

	}

	public void updateViewPort() {
		int width = Starter.getInst().getDisplaySystem().getWidth();
		int height = Starter.getInst().getDisplaySystem().getHeight();

		camera.setPerspectiveFrustum(60, (double)width / (double)height, 1, 1000);
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
		if(x >= 0 && x < dim - 1 && z >= 0 && z < dim - 1){
			mat.translate(0,
				tb.getHeight(x,z) - 1,
				0);
		}
		playerNode.setLocalTranslation(mat);

	}

	public void update(float elapsedTime) {
		//inputController.update(elapsedTime);
		updateHeight();
		cameraController.update(elapsedTime);
	}

	public ICamera getCamera() {
		return camera;
	}

	public SceneNode getSceneNode() {
		return playerNode;
	}

	public Vector3D getPosition() {
		return playerNode.getWorldTranslation().getCol(3);
	}

}
