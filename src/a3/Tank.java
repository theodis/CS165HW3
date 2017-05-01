package a3;

import graphicslib3D.*;
import sage.scene.*;
import sage.scene.shape.*;
import java.util.*;
import java.awt.Color;

public class Tank extends Group {

	public Group topGroup;
	public Group turretGroup;

	public Cube base;
	public Cube top;
	public Cylinder turret;

	public float topRotation = 0.0f;
	public float turretPitch = 0.0f;
	public float shootPower = 15.0f;

	public Tank() {
		topGroup = new Group();
		turretGroup = new Group();

		base = new Cube();
		top = new Cube();
		turret = new Cylinder(true);

		Matrix3D turretRot = turret.getLocalRotation();
		turretRot.rotateY(90);

		Matrix3D turretScale = turret.getLocalScale();
		turretScale.scale(0.25,0.25,5);

		Matrix3D turretTrans = turretGroup.getLocalTranslation();
		turretTrans.translate(0,1,0);

		Matrix3D topTrans = topGroup.getLocalTranslation();
		topTrans.translate(0,1,0);

		Matrix3D topScale = top.getLocalScale();
		topScale.scale(1.5,1,1);

		Matrix3D baseScale = base.getLocalScale();
		baseScale.scale(4,1,2);

		turretGroup.addChild(turret);

		topGroup.addChild(top);
		topGroup.addChild(turretGroup);

		addChild(base);
		addChild(topGroup);

		scale(0.2f,0.2f,0.2f);
	}

	private Vector<SceneNode> getChildRecursive(Group g) {
		Vector<SceneNode> ret = new Vector<SceneNode>();
		for(SceneNode n : g) {
			ret.add(n);
			if(n instanceof Group)
				ret.addAll(0, getChildRecursive((Group)n));
		}
		return ret;
	}

	public void scale(float x, float y, float z) {
		Vector<SceneNode> nodes = getChildRecursive(this);

		for(SceneNode n : nodes){
			Matrix3D trans = n.getLocalTranslation();
			Vector3D v = trans.getCol(3);
			v.setX(v.getX() * x);
			v.setY(v.getY() * y);
			v.setZ(v.getZ() * z);
			trans.setCol(3,v);
		}

		Matrix3D scale = getLocalScale();
		scale.scale(x,y,z);
	}

	public void setColor(Color c) {
		turret.setColor(c);
	}

	public void rotateTop(float angle) {
		setTopRotation(topRotation + angle);
	}

	public void setTopRotation(float angle) {
		topRotation = angle;

		Matrix3D rot = new Matrix3D();
		rot.rotateY(angle);

		topGroup.setLocalRotation(rot);
	}

	public void pitchTurret(float angle) {
		setTurretPitch(turretPitch + angle);
	}
	
	public void setTurretPitch(float angle) {
		if(angle < 0) angle = 0;
		if(angle > 90) angle = 90;

		turretPitch = angle;

		Matrix3D rot = new Matrix3D();
		rot.rotateZ(angle);

		turretGroup.setLocalRotation(rot);

	}

	public void adjustPower(float power) {
		setShootPower(shootPower + power);
	}

	public void setShootPower(float power) {
		shootPower = power;
		if(shootPower < 2.0f) shootPower = 2.0f;
		if(shootPower > 20.0f) shootPower = 20.0f;
	}

	public float getTopRotation() { return topRotation; }
	public float getTurretPitch() { return turretPitch; }
	public float getShootPower() { return shootPower; }

	public void fire() {
		Vector3D pos = turret.getWorldTranslation().getCol(3);
		double alpha = ((360 - topRotation) * Math.PI) / 180;
		double beta = (turretPitch * Math.PI) / 180;
		double x = Math.cos(alpha) * Math.cos(beta);
		double z = Math.sin(alpha) * Math.cos(beta);
		double y = Math.sin(beta);
		Vector3D vel = new Vector3D(x,y,z);
		vel.scale(shootPower);

		//Move the bomb forward a bit
		vel.scale(1/100.0);
		pos = pos.add(vel);
		vel.scale(100/1.0);
		Starter.getInst().addBomb(pos,vel);
		Starter.getInst().getClient().sendFireMessage(pos,vel);
	}
}
