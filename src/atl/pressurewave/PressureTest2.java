package atl.pressurewave;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;

import java.util.ArrayList;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;


public class PressureTest2 {

	myVector[][] grid; 
	boolean[][] wall;

	final double PURE_COLOR_PRESSURE_THRESHOLD = 100;
	final float[] WALL_COLOR = { 1, 0.5f, 0 };
	final int MOUSE_POWER = 10000;
	
	final double DIVISIVE_FACTOR = 1.2;
	// when the pressure is +PCPT, color will be pure white.
	// when pressure is -PCPT, color will be pure black.
	// when pressure is 0, color will be between, gray.

	final double DETERMINATION = 2;
	// determines likelihood of a given cell giving pressure to a cell not in its direction of travel
	final Orientation[] ORS = { Orientation.N, Orientation.S, Orientation.E,
			Orientation.W, Orientation.NE, Orientation.NW, Orientation.SE,
			Orientation.SW };
	final int NUM_ORS = 8;
	// ORS is short for orientations. Meaning neighbor positions. Silly.

	private boolean mouseEnabled = true;
	final int MOUSE_ENABLE_KEY = Keyboard.KEY_E;
	final int MOUSE_DISABLE_KEY = Keyboard.KEY_D;
	final int PAUSE_KEY = Keyboard.KEY_P;
	final int UNPAUSE_KEY = Keyboard.KEY_O;

	private final int FRAME_RATE_SYNC = 60;

	public static final int SCREEN_WIDTH = 320;
	public static final int SCREEN_HEIGHT = 240;

	private boolean paused = false;

	private final String TITLE = "Pressure Waves, trial 2";

	private long lastFrame; // used if using delta to adjust speeed

	private long getTime() {
		return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}

	/**
	 * Delta is a value used to track the framerate and adjust speed of
	 * interactions to ensure that things happen consistently across machines
	 * with different speeds.
	 * 
	 * @return integer
	 */
	/*
	 * private int getDelta() { long currentTime = getTime(); int delta = (int)
	 * (currentTime - lastFrame); lastFrame = currentTime; return delta; }
	 */

	private void setUpDisplay() {
		// initialization for Display
		try {
			Display.setDisplayMode(new DisplayMode(SCREEN_WIDTH, SCREEN_HEIGHT));
			Display.setTitle(TITLE);
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
	}

	private void setUpOpenGL() {
		// initialization for openGl

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, SCREEN_WIDTH, 0, SCREEN_HEIGHT, 1, -1);

		// 2d, no perspective, center at 0,0
		// glOrtho(WIDTH/2.0, -WIDTH / 2.0, HEIGHT/2.0, -HEIGHT/2.0, 1, -1);
		glMatrixMode(GL_MODELVIEW);
	}

	private void setUpTimer() {
		lastFrame = getTime();
	}

	private myVector[][] setUpGrid(int w, int h) {
		myVector[][] g = new myVector[w][h];
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				// g[i][j] = (int) (((w - i) * 1.0 / w) *
				// PURE_COLOR_PRESSURE_THRESHOLD);
				// a gradient of pressures, high -> low

				// g[i][j] = 0;

				if (j < 20) {
					g[i][j] = new myVector(PURE_COLOR_PRESSURE_THRESHOLD);
				}
				else if (j > 460) {
					g[i][j] = new myVector(-PURE_COLOR_PRESSURE_THRESHOLD);
				}
				else {
					g[i][j] = new myVector(0);
				}
				// strips of low at top and high at bottom
			}
		}
		return g;
	}

	private boolean[][] setUpWall(int w, int h) {
		boolean[][] wa = new boolean[w][h];
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				wa[i][j] = false;
				if (i > 200 && i < 230 && j < 300)
					wa[i][j] = true;
			}
		}
		return wa;
	}

	public PressureTest2() {
		setUpDisplay();
		setUpOpenGL();

		setUpTimer();
		

		grid = setUpGrid(SCREEN_WIDTH, SCREEN_HEIGHT);
		wall = setUpWall(SCREEN_WIDTH, SCREEN_HEIGHT);
		// int delta;

		System.out.println("lastFrame = "+lastFrame);
		
		while (!Display.isCloseRequested()) {
			// loop
			// delta = getDelta();

			tick();
			input();
			render();

			Display.update();
			Display.sync(FRAME_RATE_SYNC);
		}

		Display.destroy();
		System.exit(0);
	}

	private void tick() {
		// tick world.
		if (paused) {
			return;
		}
		// make a copy of the grid to manipulate values on
		myVector[][] gridcpy = new myVector[SCREEN_WIDTH][SCREEN_HEIGHT];
		for (int i = 0; i < SCREEN_WIDTH; i++) {
			for (int j = 0; j < SCREEN_HEIGHT; j++) {
				gridcpy[i][j] = grid[i][j];
			}
		}

		// actually do manipulation of values
		for (int i = 0; i < SCREEN_WIDTH; i++) {
			for (int j = 0; j < SCREEN_HEIGHT; j++) {
				balanceWithNeighbors(i, j, gridcpy);
			}
		}
		grid = gridcpy;
		return;
	}

	private boolean isAcceptableBalanceTarget(int x1, int y1, int x2, int y2) {
		if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0 || x1 >= SCREEN_WIDTH
				|| x2 >= SCREEN_WIDTH || y1 >= SCREEN_HEIGHT
				|| y2 >= SCREEN_HEIGHT) // is it out of bounds?
			return false;
		if (wall[x2][y2] || wall[x1][y1]) // is it a wall?
			return false;
		return grid[x1][y1].getVal() > grid[x2][y2].getVal();
	}

	private boolean setValue(int x, int y, double val, myVector[][] g) {
		if (wall[x][y]) {
			return false;
		}
		g[x][y].setVal(val);
		return true;
	}

	private boolean balanceWithNeighbors(int x, int y, myVector[][] gc) {
		double totalNeighborBalanceCoefficient = 0;
		ArrayList<Orientation> goodors = new ArrayList<PressureTest2.Orientation>();
		for (int i = 0; i < NUM_ORS; i++) {
			if (isAcceptableBalanceTarget(x, y, x + getXShift(ORS[i]), y
					+ getYShift(ORS[i]))) {
				//process with random weighting whether or not to balance with this one based on past orientation, 'speed'
				if(evaluateWeightOfOrient(ORS[i],gc[x][y].getDir(),x,y,gc))
					goodors.add(ORS[i]);
				totalNeighborBalanceCoefficient = getBalanceValue(ORS[i]);
			}
		}

		for (int i = 0; i < goodors.size(); i++) {
			balanceWithOrientation(x, y, goodors.get(i), gc); 
		}

		return totalNeighborBalanceCoefficient > 0;
	}
	
	private boolean evaluateWeightOfOrient(Orientation to, Orientation from, int x, int y, myVector[][] gc)
	{
		switch (to) {
		case C:
			return true;
		case E:
			switch (from){
			case E:
				return true;
			case NE:
				return Math.random()*gc[x][y].getChng()>DETERMINATION*3;
			case SE:
				return Math.random()*gc[x][y].getChng()>DETERMINATION*3;
			default:
				return Math.random()*gc[x][y].getChng()>DETERMINATION;
			
			}
		case N:
			switch (from){
			case N:
				//System.out.println(gc[x][y].getChng());
				return true;
			case NE:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case NW:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			default:
				return Math.random()*gc[x][y].getChng()<DETERMINATION;
			}
		case NE:
			switch (from){
			case E:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case N:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case NE:
				return true;
			default:
				return Math.random()*gc[x][y].getChng()<DETERMINATION;
			}
		case NW:
			switch (from){
			case N:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case NE:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case NW:
				return true;
			default:
				return Math.random()*gc[x][y].getChng()<DETERMINATION;
			}
		case S:
			switch (from){
			case S:
				return true;
			case SE:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case SW:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			default:
				return Math.random()*gc[x][y].getChng()<DETERMINATION;
			}
		case SE:
			switch (from){
			case E:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case S:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case SE:
				return true;
			default:
				return Math.random()*gc[x][y].getChng()<DETERMINATION;
			}
		case SW:
			switch (from){
			case S:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case SW:
				return true;
			case W:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			default:
				return Math.random()*gc[x][y].getChng()<DETERMINATION;
			}
		case W:
			switch (from){
			case NW:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case SW:
				return Math.random()*gc[x][y].getChng()<DETERMINATION*3;
			case W:
				return true;
			default:
				return Math.random()*gc[x][y].getChng()<DETERMINATION;
			}
		default:
			System.out.println("<" + to + "from" + from
					+ ">: Failed to evaluate direction wieghting: unknown orientation.");
			return false;

		}
		//return true;
	}
	
	private boolean balanceWithOrientation(int x, int y, Orientation or,
			myVector[][] gc) {
		// Still does not work as intended.
		double valueDiff = grid[x][y].getVal() - grid[x+getXShift(or)][y+getYShift(or)].getVal();
		double factor = getBalanceValue(or) * valueDiff / DIVISIVE_FACTOR;
		setValue(x, y, gc[x][y].getVal() - factor, gc);
		setValue(x + getXShift(or), y + getYShift(or), gc[x + getXShift(or)][y
				+ getYShift(or)].getVal()
				+ factor, gc);
		gc[x][y].setDir(or);
		gc[x][y].setChng(valueDiff/DIVISIVE_FACTOR);
		return true;

	}

	private void render() {
		// draw.
		glClear(GL_COLOR_BUFFER_BIT);// | GL_DEPTH_BUFFER_BIT);
		glBegin(GL_POINTS);
		for (int i = 0; i < SCREEN_WIDTH; i++) {
			for (int j = 0; j < SCREEN_HEIGHT; j++) {
				float[] colors = getColor(i, j);
				glColor3f(colors[0], colors[1], colors[2]);
				glVertex2f(i, j);
			}
		}

		glEnd();
	}

	private float[] getColor(int x, int y) {
		if (wall[x][y]) {
			return WALL_COLOR;
		}
		float[] c = new float[3];
		float univColor = 0.5f;
		univColor += (grid[x][y].getVal() * 1.0f / PURE_COLOR_PRESSURE_THRESHOLD) / 2;

		// if you want it more colorful, then you can do dat!
		c[0] = univColor;
		c[1] = univColor;
		c[2] = univColor;
		return c;
	}

	private void addPressure(int x, int y, int amount) {
		setValue(x, y, grid[x][y].getVal() + amount, grid);
	}

	// As of now, mouse still just adds or subtracts pressure, does not 'push'
	// like a true sound generator.

	private void input() {
		if (mouseEnabled) {
			int mouseX = Mouse.getX();// - WIDTH / 2;
			int mouseY = Mouse.getY();// - HEIGHT / 2;
			if (Mouse.isButtonDown(0)) {
				//System.out.println(mouseX + ", " + mouseY);
				addPressure(mouseX, mouseY, MOUSE_POWER);
			}
			if (Mouse.isButtonDown(1)) {
				//System.out.println(grid[mouseX][mouseY]);
				addPressure(mouseX, mouseY, -MOUSE_POWER);
			}
		}
		if (Keyboard.isKeyDown(MOUSE_ENABLE_KEY)) {
			mouseEnabled = true;
		}
		if (Keyboard.isKeyDown(MOUSE_DISABLE_KEY)) {
			mouseEnabled = false;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			Display.destroy();
			System.exit(0);
		}
		if (Keyboard.isKeyDown(PAUSE_KEY)) {
			paused = true;
			System.out.println("Paused");
		}
		if (Keyboard.isKeyDown(UNPAUSE_KEY)) {
			paused = false;
			System.out.println("Unpaused");
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			//
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			//
		}

	}

	public static void main(String[] args) {
		new PressureTest2();
	}

	public enum Orientation {
		N, S, E, W, NE, NW, SE, SW, C
	}

	public int getXShift(Orientation or) {
		switch (or) {
		case E:
			return 1;
		case N:
			break;
		case NE:
			return 1;
		case NW:
			return -1;
		case S:
			break;
		case SE:
			return 1;
		case SW:
			return -1;
		case W:
			return -1;
		default:
			System.out.println("<" + or
					+ ">: Failed to get X shift: unknown orientation.");
			return 0;

		}
		return 0;
	}

	public int getYShift(Orientation or) {
		switch (or) {
		case E:
			break;
		case N:
			return 1;
		case NE:
			return 1;
		case NW:
			return 1;
		case S:
			return -1;
		case SE:
			return -1;
		case SW:
			return -1;
		case W:
			break;
		default:
			System.out.println("<" + or
					+ ">: Failed to get Y shift: unknown orientation.");
			return 0;

		}
		return 0;
	}

	public double getBalanceValue(Orientation or) {
		switch (or) {
		case E:
			return 1;
		case N:
			return 1;
		case NE:
			return 1 / Math.sqrt(2);
		case NW:
			return 1 / Math.sqrt(2);
		case S:
			return 1;
		case SE:
			return 1 / Math.sqrt(2);
		case SW:
			return 1 / Math.sqrt(2);
		case W:
			return 1;
		default:
			System.out.println("<" + or
					+ ">: Failed to get balance value: unknown orientation.");
			return 0;
		}
	}
	private class myVector{ //'vector' with magnitude and a carried value
		private double value;
		private Orientation lastDir=Orientation.C;
		private double lastChng=0;
		private myVector(double sval){
			value=sval;
		}
		private double getVal(){
			return value;
		}
		private Orientation getDir(){
			return lastDir;
		}
		private double getChng(){
			return lastChng;
		}
		private void setVal(double val){
			value=val;
		}
		private void setDir(Orientation dir){
			lastDir=dir;
		}
		private void setChng(double chng){
			lastChng=chng;
		}
		
	}

}
