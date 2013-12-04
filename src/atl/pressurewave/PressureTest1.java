package atl.pressurewave;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class PressureTest1 {

	int[][] grid;
	boolean[][] wall;

	final int PURE_COLOR_PRESSURE_THRESHOLD = 100;
	final float[] WALL_COLOR = { 1, 0.5f, 0 };
	// when the pressure is +PCPT, color will be pure white.
	// when pressure is -PCPT, color will be pure black.
	// when pressure is 0, color will be between, gray.

	private boolean mouseEnabled = true;
	final int MOUSE_ENABLE_KEY = Keyboard.KEY_E;
	final int MOUSE_DISABLE_KEY = Keyboard.KEY_D;
	private final int FRAME_RATE_SYNC = 60;

	public static final int SCREEN_WIDTH = 640;
	public static final int SCREEN_HEIGHT = 480;

	private final String TITLE = "Pressure Waves!";

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

	private int[][] setUpGrid(int w, int h) {
		int[][] g = new int[w][h];
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				//g[i][j] = (int) (((w - i) * 1.0 / w) * PURE_COLOR_PRESSURE_THRESHOLD);
				// a gradient of pressures, high -> low
				
				g[i][j] = 0;
				
				
				/*if(j < 20){
					g[i][j] = PURE_COLOR_PRESSURE_THRESHOLD;
				}
				if(j > 460){
					g[i][j] = -PURE_COLOR_PRESSURE_THRESHOLD;
				}*/
				//strips of low at top and high at bottom
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

	public PressureTest1() {
		setUpDisplay();
		setUpOpenGL();

		setUpTimer();

		grid = setUpGrid(SCREEN_WIDTH, SCREEN_HEIGHT);
		wall = setUpWall(SCREEN_WIDTH, SCREEN_HEIGHT);
		// int delta;

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

		// make a copy of the grid to manipulate values on
		int[][] gridcpy = new int[SCREEN_WIDTH][SCREEN_HEIGHT];
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
				|| y2 >= SCREEN_HEIGHT)
			return false;
		if (wall[x2][y2] || wall[x1][y1])
			return false;
		return grid[x1][y1] > grid[x2][y2];
	}

	private boolean setValue(int x, int y, int val, int[][] g) {
		if (wall[x][y]) {
			return false;
		}
		g[x][y] = val;
		return true;
	}

	private boolean balanceWithNeighbors(int x, int y, int[][] gc) {	
		//Does not work as intended.
		boolean balanced = false;
		if (balanceWithCell(x, y, x - 1, y, gc))
			balanced = true;
		if (balanceWithCell(x, y, x, y - 1, gc))
			balanced = true;
		if (balanceWithCell(x, y, x + 1, y, gc))
			balanced = true;
		if (balanceWithCell(x, y, x, y + 1, gc))
			balanced = true;
		return balanced;
	}

	private boolean balanceWithCell(int x1, int y1, int x2, int y2, int[][] gc) {
		//Does not work as intended.
		if (isAcceptableBalanceTarget(x1, y1, x2, y2)) {
			int valueDiff = grid[x1][y1] - grid[x2][y2];
			double root = Math.sqrt(valueDiff);
			setValue(x1, y1, gc[x1][y1] - (int)root, gc);
			setValue(x2, y2, gc[x2][y2] + (int)root, gc);
			return true;
		}
		return false;
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
		univColor += (grid[x][y] * 1.0f / PURE_COLOR_PRESSURE_THRESHOLD) / 2;

		// if you want it more colorful, then you can do dat!
		c[0] = univColor;
		c[1] = univColor;
		c[2] = univColor;
		return c;
	}

	private void input() {
		if (mouseEnabled) {
			int mouseX = Mouse.getX();// - WIDTH / 2;
			int mouseY = Mouse.getY();// - HEIGHT / 2;
			if (Mouse.isButtonDown(0)) {
				System.out.println(mouseX + ", " + mouseY);
				setValue(mouseX, mouseY, grid[mouseX][mouseY] + 100, grid);
			}
			if (Mouse.isButtonDown(1)) {
				System.out.println(grid[mouseX][mouseY]);
				setValue(mouseX, mouseY, grid[mouseX][mouseY] - 100, grid);
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
		if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			//
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			//
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			//
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			//
		}

	}

	public static void main(String[] args) {
		new PressureTest1();

	}

}
