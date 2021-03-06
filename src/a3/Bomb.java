package a3;

import sage.scene.shape.Cylinder;
import sage.scene.shape.Sphere;
import java.awt.Color;
import graphicslib3D.*;
import sage.terrain.*;

public class Bomb extends Sphere {
	public Vector3D velocity;
	public Vector3D position;
	public Tank source;

	public Tank getSource() {return source;}
	
	public Bomb(Tank s, Vector3D initialPosition, Vector3D initialVelocity) {
		this.setColor(Color.BLACK);
		velocity = initialVelocity;
		position = initialPosition;
		source = s;

		Matrix3D scale = getLocalScale();
		scale.scale(0.03f,0.03f,0.03f);
	}

	public void update(float elapsedTime) {
		Point3D start = new Point3D(position.getX(), position.getY(), position.getZ());

		//Apply gravity
		double y = velocity.getY();
		y -= elapsedTime / 200.0f;
		velocity.setY(y);

		Vector3D scaled = new Vector3D(velocity.getX(), velocity.getY(), velocity.getZ());
		scaled.scale(elapsedTime/1000);
		position = position.add(scaled);

		Matrix3D pos = new Matrix3D();
		pos.translate(position.getX(), position.getY(), position.getZ());
		setLocalTranslation(pos);

		//See if it's time to remove
		TerrainBlock tb = Starter.getInst().getTerrain();
		float ty = tb.getHeight((float)position.getX(), (float)position.getZ()) - 2;
		if(position.getY() < -5 || position.getY() < ty){
			Starter.getInst().removeBomb(this);
		}
		
		Point3D end = new Point3D(position.getX(), position.getY(), position.getZ());
		Starter.getInst().addTrail(start,end,source.getColor());

	}
}
