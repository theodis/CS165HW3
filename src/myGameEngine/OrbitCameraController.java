package myGameEngine;

import graphicslib3D.*;
import sage.camera.ICamera;
import sage.camera.controllers.AbstractCameraController;
import sage.input.IInputManager;
import sage.scene.SceneNode;

public class OrbitCameraController extends AbstractCameraController {

	private float azimuth = 0.0f;
	private float altitude = 0.0f;
	private float radius = 10.0f;

	public void update(float elapsedTime){
		ICamera c = getCamera();
		SceneNode sn = getTarget();
		if(sn != null && c != null){
			double dx = radius * Math.cos(altitude) * Math.sin(azimuth);
			double dy = radius * Math.sin(altitude);
			double dz = radius * Math.cos(altitude) * Math.cos(azimuth);

			Vector3D targetVec = sn.getWorldTranslation().getCol(3);
			Point3D lookatPos = new Point3D(targetVec.add(new Vector3D(0,1,0)));
			Point3D cameraPos = new Point3D(targetVec.add(new Vector3D(dx,dy,dz)));

			c.setLocation(cameraPos);
			c.lookAt(lookatPos, new Vector3D(0,1,0));
		}
	}

	public void Rotate(float degrees){
		azimuth += degrees;
		while(azimuth < 0) azimuth += Math.PI * 2;
		while(azimuth >= Math.PI * 2) azimuth -= Math.PI * 2;
	}

	public void Pitch(float degrees){
		altitude += degrees;
		if(altitude > (Math.PI / 2) * 0.9f) altitude = ((float)Math.PI / 2) * 0.9f;
		if(altitude < 0.0f) altitude = 0.0f;
	}

	public void adjustRadius(float amount){
		radius += amount;
		if(radius < 3.0f) radius = 3.0f;
		if(radius > 100.f) radius = 100.f;
	}

	public void setRadius(float amount){
		radius = amount;
	}

	public float getRadius() { return radius; }

	public void setInputMode(IInputManager im, String controllerName) {

	}
}
