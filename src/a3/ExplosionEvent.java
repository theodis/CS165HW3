package a3;

import sage.event.*;
import graphicslib3D.*;

public class ExplosionEvent extends AbstractGameEvent {
		private Tank source;
		private Vector3D origin;
		private float radius;
	
		public ExplosionEvent(Tank s, Vector3D o, float r){
			source = s;
			origin = o;
			radius = r;
		}
		
		public Tank getSource() {return source;}
		public Vector3D getOrigin() {return origin;}
		public float getRadius() {return radius;}
}