package a3;

import graphicslib3D.*;
import sage.scene.SceneNode;

public class Ghost {

	public Tank node;
	
	public Ghost(float x, float y, float z) {
		node = new Tank();
		move(x,y,z);
	}

	public void move(float x, float y, float z) {
		Matrix3D mat = new Matrix3D();
		mat.translate(x,y,z);

		node.setLocalTranslation(mat);
	}

	public Tank getNode() {
		return node;
	}
}
