package a3;

import java.awt.Color;
import myGameEngine.*;

public class JSVars {

	private static JSVars inst;

	public static JSVars getInst() {
		if(inst == null)
			inst = new JSVars();
		return inst;
	}

	private Color tankColor;
	private boolean fullScreen;
	private int width;
	private int height;

	public Color getTankColor() { return tankColor; }
	public boolean getFullScreen() { return fullScreen; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }

	private JSVars() { 
		JSEngine.getInst().execute("vars.js");
		Double r = (Double)JSEngine.getInst().getEngine().get("r");
		Double g = (Double)JSEngine.getInst().getEngine().get("g");
		Double b = (Double)JSEngine.getInst().getEngine().get("b");
		tankColor = new Color(r.floatValue(),g.floatValue(),b.floatValue());
		
		width = ((Integer)JSEngine.getInst().getEngine().get("width")).intValue();
		height = ((Integer)JSEngine.getInst().getEngine().get("height")).intValue();

		fullScreen = (Boolean)JSEngine.getInst().getEngine().get("fullscreen");
	}
}
