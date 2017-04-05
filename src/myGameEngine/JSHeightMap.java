package  myGameEngine;

import sage.terrain.AbstractHeightMap;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JSHeightMap extends AbstractHeightMap {
	//private double[][] heights;

	public JSHeightMap(String script, String ret) {
		JSEngine.getInst().execute(script);
		ScriptObjectMirror som = (ScriptObjectMirror)JSEngine.getInst().getEngine().get(ret);
		int height = som.keySet().size();
		int width = ((ScriptObjectMirror)som.get(som.keySet().iterator().next())).keySet().size();
		heightData = new float[width * height];
		edgeSize = width;
		heightScale = 1;

		int x,y;
		y = 0;
		for(Object key : som.keySet()){
			ScriptObjectMirror row = (ScriptObjectMirror)som.get(key);
			x = 0;
			for(Object ikey : row.keySet()){
				heightData[y * width + x] = ((Double)row.get(ikey)).floatValue() * 10;
				x++;
			}
			y++;
		}
	}

	public boolean load() {
		return true;
	}
}
