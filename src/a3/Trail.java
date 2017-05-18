package a3;

import sage.scene.shape.Line;
import java.awt.Color;
import graphicslib3D.*;
import sage.terrain.*;
import sage.event.*;

public class Trail extends Line {
	private float max = 2000.0f;
	private float time;

	public Trail(Point3D a, Point3D b, Color c ) {
		super(a,b,c,1);
		time = 0;
	}

	public void update(float elapsedTime) {
		time += elapsedTime;
	}

	public float getTime() { return time; }
	public float getMax() { return max; }
}
