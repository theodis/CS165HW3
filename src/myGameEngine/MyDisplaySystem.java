package myGameEngine;

import java.awt.Window;
import java.awt.GraphicsDevice;
import java.awt.Canvas;
import javax.swing.JFrame;
import sage.renderer.IRenderer;
import sage.renderer.RendererFactory;
import sage.display.IDisplaySystem;
import sage.display.DisplaySystem;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class MyDisplaySystem implements IDisplaySystem {

	private IRenderer renderer;
	private GraphicsDevice device;
	private boolean isCreated;
	private boolean isFullScreen;
	private Canvas canvas;
	private JFrame frame;

	private int width, height, bitDepth, refreshRate;

	public void setCustomCursor(String str) {

	}

	public void setPredefinedCursor(int i) {

	}

	public void convertPointToScreen(Point p) {

	}

	public boolean isShowing() {
		return true;
	}

	public void addMouseMotionListener(MouseMotionListener l) {

	}

	public void addMouseListener(MouseListener l) {

	}

	public void addKeyListener(KeyListener l) {

	}

	public boolean isFullScreen() { return isFullScreen; }
	public boolean isCreated() { return isCreated; }
	public IRenderer getRenderer() { return renderer; }
	public void setTitle(String title) { frame.setTitle(title); }
	public void setRefreshRate(int r) { refreshRate = r; }
	public void setBitDepth(int d) { bitDepth = d; }
	public void setHeight(int h) { height = h; }
	public void setWidth(int w) { width = w; }
	public int getRefreshRate() { return refreshRate; }
	public int getBitDepth() { return bitDepth; }
	public int getHeight() { return height; }
	public int getWidth() { return width; }

	public MyDisplaySystem(int w, int h, int d, int r, boolean fs, String name) {
		width = w;
		height = h;
		bitDepth = d;
		refreshRate = r;
		isFullScreen = fs;

		renderer = RendererFactory.createRenderer(name);
		if(renderer == null)
			throw(new RuntimeException("Failed to create renderer."));

		canvas = renderer.getCanvas();
		frame = new JFrame("Default Title");
		frame.add(canvas);
		
		GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		device = environment.getDefaultScreenDevice();

		DisplayMode displayMode = null;

		if(fs){ //For fullscreen just use the current display mode
			displayMode = device.getDisplayMode();
			width = displayMode.getWidth();
			height = displayMode.getHeight();
			bitDepth = displayMode.getBitDepth();
			refreshRate = displayMode.getRefreshRate();
		} else
			displayMode = new DisplayMode(width, height, bitDepth, refreshRate);

		if(device.isFullScreenSupported() && fs) {
			frame.setUndecorated(true);
			frame.setResizable(false);
			frame.setIgnoreRepaint(true);

			device.setFullScreenWindow(frame);

			if(displayMode != null && device.isDisplayChangeSupported()) {
				try {
					device.setDisplayMode(displayMode);
					frame.setSize(displayMode.getWidth(), displayMode.getHeight());
				} catch( Exception e) {
					System.err.println("Exception setting DisplayMode: " + e);
				}
			} else {
				System.err.println("Cannot set display mode");
			}
		} else {
			isFullScreen = false;
			frame.setSize(displayMode.getWidth(), displayMode.getHeight());
			frame.setLocationRelativeTo(null);
		}

		DisplaySystem.setCurrentDisplaySystem((IDisplaySystem)this);
		frame.setVisible(true);
		isCreated = true;
	}

	public void close() {
		if(device != null) {
			Window window = device.getFullScreenWindow();
			if(window != null) {
				window.dispose();
			}

			device.setFullScreenWindow(null);
		}
	}
}
