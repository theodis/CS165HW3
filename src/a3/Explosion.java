package a3;

import sage.scene.shape.Sphere;
import java.awt.Color;
import graphicslib3D.*;
import sage.terrain.*;
import sage.event.*;

public class Explosion extends Sphere {
	public class RemoveExplosionEvent extends AbstractGameEvent {
		private Explosion source;
	
		public RemoveExplosionEvent(Explosion s){
			source = s;
		}
		
		public Explosion getSource() {return source;}
	}

	public float size;
	public float max;

	public Explosion(Vector3D initialPosition, float m) {
		this.setColor(Color.RED);
		size = 0.1f;
		max = m;

		Matrix3D trans = getLocalTranslation();
		trans.setCol(3, initialPosition);
		Matrix3D scale = getLocalScale();
		scale.scale(size,size,size);
	}

	public void update(float elapsedTime) {
		size += elapsedTime / 40;
		Matrix3D scale = new Matrix3D();
		scale.scale(size,size,size);
		setLocalScale(scale);
		if(size > max)
			Starter.getInst().getEventManager().triggerEvent(new RemoveExplosionEvent(this));
	}
}
