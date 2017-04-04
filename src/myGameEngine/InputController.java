package  myGameEngine;

import graphicslib3D.*;
import sage.input.IInputManager;
import sage.input.action.AbstractInputAction;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Event;
import sage.scene.SceneNode;
import sage.scene.Controller;
import sage.camera.ICamera;

public class InputController extends Controller {

	private IInputManager input;
	private ICamera camera;

	//Movement along each of the camera axis
	private float mn; 
	private float mu;

	//Speed of the object
	private float speed;

	public void update(double elapsedTime) {
		if(mn != 0 || mu != 0){
			//Figure out the movement direction based on
			
			//Get the pertinent camera vectors
			Vector3D n = camera.getViewDirection();
			Vector3D u = camera.getRightAxis();

			//Remove any altitude component
			n.setY(0);
			u.setY(0);

			//Make them unit vectors
			n.scale(1/n.magnitude());
			u.scale(1/u.magnitude());

			//Scale by the speed, elapsed time, and magnitude in each direction
			n.scale(speed * elapsedTime / 1000 * mn);
			u.scale(speed * elapsedTime / 1000 * mu);

			//Build final movement vector
			Vector3D move = n.add(u);

			//Build rotation matrix
			Matrix3D rotate = new Matrix3D();
			double angle = Math.atan2(move.getZ(), move.getX()) * (180 / Math.PI);
			rotate.rotateY(-angle);

			//Apply the movement to all connected scenenodes
			for(SceneNode sn : controlledNodes) {
				Matrix3D pos = sn.getLocalTranslation();
				pos.translate(move.getX(), move.getY(), move.getZ());
				sn.setLocalRotation(rotate);
			}

			//Clear movement values for next frame
			mn = 0;
			mu = 0;
		}
	}

	public InputController(IInputManager i, ICamera c, float s) {
		input = i;
		camera = c;
		speed = s;
	}

	public void bindToController(net.java.games.input.Controller c) {
		switch(c.getType().toString()) {
			case "Keyboard":
				input.associateAction(c, Identifier.Key.W, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						mn = 1;
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Key.A, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						mu = 1;
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Key.S, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						mn = -1;
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Key.D, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						mu = -1;
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				break;
			case "Gamepad":
				input.associateAction(c, Identifier.Axis.Y, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						mn = -1 * event.getValue();
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				input.associateAction(c, Identifier.Axis.X, 
					new AbstractInputAction(){public void performAction(float time, Event event) {
						mu = event.getValue();
					}}, IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				break;
		}
	}
}
