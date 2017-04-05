package a3;

import graphicslib3D.*;
import sage.scene.SceneNode;
import sage.scene.shape.Pyramid;

public class Ghost {

	public SceneNode node;
	
	public Ghost(float x, float y, float z) {
		node = new Pyramid();
		move(x,y,z);
	}

	public void move(float x, float y, float z) {
		Matrix3D mat = new Matrix3D();
		mat.translate(x,y,z);

		node.setLocalTranslation(mat);
	}

	public SceneNode getNode() {
		return node;
	}
}
