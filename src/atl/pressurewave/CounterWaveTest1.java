package atl.pressurewave;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;

import java.util.ArrayList;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.Color;

public class CounterWaveTest1 {

	ArrayList<Pillar> pills;
	final int NUM_PILLS = 100;
	final int TICK_DELAY = 2;

	final float LEFTCOLOR[] = { 1f, 0f, 0f };
	final float RIGHTCOLOR[] = { 0f, 0f, 1f };
	final float MIXCOLOR[] = {1f, 1f, 1f};
	final float NULLCOLOR[] = {.5f, .5f, .5f};
	
	final double POWER_LIMIT = 20;
	
	double leftPower = 0;
	double rightPower = 0;
	
	double powerStepdown = 0.1;
	
	int tick = 0;

	private boolean mouseEnabled = true;
	final int MOUSE_ENABLE_KEY = Keyboard.KEY_E;
	final int MOUSE_DISABLE_KEY = Keyboard.KEY_D;
	private final int FRAME_RATE_SYNC = 60;

	public static final int SCREEN_WIDTH = 640;
	public static final int SCREEN_HEIGHT = 480;

	private final String TITLE = "'Murica!";

	// private long lastFrame; // used if using delta to adjust speeed

	/*
	 * private long getTime() { return (Sys.getTime() * 1000) /
	 * Sys.getTimerResolution(); }
	 */

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

	/*
	 * private void setUpTimer() { lastFrame = getTime(); }
	 */

	private void setUpPillars() {
		pills = new ArrayList<Pillar>();
		for (int i = 0; i < NUM_PILLS; i++) {
			pills.add(new Pillar());
		}
	}

	private void displayPillarStatus() {
		System.out.println("---Pillar Status---");
		for (int i = 0; i < NUM_PILLS; i++) {
			System.out.println(pills.get(i));
		}
		System.out.println("---End Status---");
	}

	public CounterWaveTest1() {
		setUpDisplay();
		setUpOpenGL();

		// setUpTimer();

		// int delta;

		setUpPillars();
		
		
		if(powerStepdown < 0){
			System.out.println("Warning: powerStepdown is " + powerStepdown);
		}

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
		tick++;

		if (tick > TICK_DELAY) {
			tick = 0;
			//displayPillarStatus();
			propagateLeft();
			propagateRight();
		}
		// tick world.
		return;
	}
	
	
	private void sendSignal(){
		sendLeftSignal();
		sendRightSignal();
	}
	
	private void sendLeftSignal(){
		Pillar p = pills.get(0);
		p.setLeftStrength(p.getLeftStrength() + leftPower);
	}
	
	private void sendRightSignal(){
		Pillar p = pills.get(NUM_PILLS - 1);
		p.setRightStrength(p.getRightStrength() + rightPower);
	}
	
	private void downStepBothPowers(){
		downStepLeftPower();
		downStepRightPower();
	}
	
	private double getPowerDown(double pow){
		double changed = pow;
		
		if(Math.abs(pow) > powerStepdown){
			if(pow > 0){
				changed -= powerStepdown;
			}
			else{
				changed += powerStepdown;
			}
		}
		else{
			changed = 0;
		}
		
		return changed;
	}
	
	private void downStepLeftPower(){
		leftPower = getPowerDown(leftPower);
	}
	
	private void downStepRightPower(){
		rightPower = getPowerDown(rightPower);
	}

	private void propagateLeft() {
		double stor[] = new double[NUM_PILLS];

		for (int i = 0; i < NUM_PILLS - 1; i++) {
			stor[i] = pills.get(i).getLeftStrength();
		}

		pills.get(0).setLeftStrength(0.0);
		for (int i = 1; i < NUM_PILLS; i++) {
			pills.get(i).setLeftStrength(stor[i - 1]);
		}
	}

	private void propagateRight() {
		double stor[] = new double[NUM_PILLS];

		for (int i = NUM_PILLS - 1; i > 0; i--) {
			stor[i] = pills.get(i).getRightStrength();
		}

		pills.get(NUM_PILLS - 1).setRightStrength(0.0);

		for (int i = NUM_PILLS - 2; i >= 0; i--) {
			pills.get(i).setRightStrength(stor[i + 1]);
		}
	}

	private void render() {
		// draw.

		//renderIndividual();

		renderConnected();
		
		// glEnd();
	}

	private void renderConnected() {
		glClear(GL_COLOR_BUFFER_BIT);

		renderConnectedSinglePath(-1); 	//left
		renderConnectedSinglePath(1); 	//right
		renderConnectedSinglePath(0);	//sum
	}

	/**
	 * 
	 * @param path designates which path it is, IE left, right, or summation
	 */
	private void renderConnectedSinglePath(int path) {
		glBegin(GL_LINE_STRIP);
		switch(path){
		case -1:	//Left path designated
			glColor3f(LEFTCOLOR[0], LEFTCOLOR[1], LEFTCOLOR[2]);
			break;
		case 0:
			glColor3f(MIXCOLOR[0], MIXCOLOR[1], MIXCOLOR[2]);
			break;
		case 1:
			glColor3f(RIGHTCOLOR[0], RIGHTCOLOR[1], RIGHTCOLOR[2]);
			break;
		default:
			System.out.println("Error: Unknown path type:" + path + ", using NULLCOLOR.");
			glColor3f(NULLCOLOR[0], NULLCOLOR[1], NULLCOLOR[2]);
			break;
		}	
		for (int i = 0; i < pills.size(); i++) {
			glVertex2d((int) (i * (SCREEN_WIDTH * 1.0) / NUM_PILLS), pills.get(i).getScreenParameterPos(path));
		}
		
		glEnd();
	}

	
	/**
	 * Deprecated. This was the odd linebar thing from previously.
	 */
	private void renderIndividual() {
		glClear(GL_COLOR_BUFFER_BIT);// | GL_DEPTH_BUFFER_BIT);
		// glBegin(GL_POINTS);
		for (int i = 0; i < pills.size(); i++) {
			pills.get(i)
					.renderPillar((int) (i * (SCREEN_WIDTH * 1.0) / NUM_PILLS),
							(int) ((i + 1) * (SCREEN_WIDTH * 1.0) / NUM_PILLS),
							0, 0, 0);
		}

	}

	private void input() {
		if (mouseEnabled) {
			int mouseX = Mouse.getX();// - WIDTH / 2;
			int mouseY = Mouse.getY();// - HEIGHT / 2;
			if (Mouse.isButtonDown(0)) {
				Pillar p = pills.get(0);
				p.setLeftStrength(p.getLeftStrength() + 1.0);
			}
			if (Mouse.isButtonDown(1)) {
				Pillar p = pills.get(NUM_PILLS - 1);
				p.setRightStrength(p.getRightStrength() - 1.0);
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

	private class Pillar {
		private double leftStrength;
		private double rightStrength;

		public Pillar() {
			leftStrength = 0;
			rightStrength = 0;
		}

		public double getLeftStrength() {
			return leftStrength;
		}

		public void setLeftStrength(double leftStrength) {
			this.leftStrength = leftStrength;
		}

		public double getRightStrength() {
			return rightStrength;
		}

		public void setRightStrength(double rightStrength) {
			this.rightStrength = rightStrength;
		}
		
		public double getScreenParameterPos(int param){
			switch(param){
			case -1:
				return getScreenLSPos();
			case 0:
				return getScreenLRSPos();
			case 1:
				return getScreenRSPos();
			default:
				System.out.println("Error: Tried to get screen height of unknown param: " + param);
				return 0;
			}
		}

		public double getScreenLSPos() {
			return (SCREEN_HEIGHT / 2)
					* (1 + (getLeftStrength() / POWER_LIMIT));
		}

		public double getScreenRSPos() {
			return (SCREEN_HEIGHT / 2)
					* (1 + (getRightStrength() / POWER_LIMIT));
		}

		public double getScreenLRSPos() {
			return (SCREEN_HEIGHT / 2)
					* (1 + ((getRightStrength() + getLeftStrength()) / POWER_LIMIT));
		}

		public void renderPillar(int leftBound, int rightBound, double r,
				double g, double b) {
			// System.out.println("HADUHGF");
			glColor3f((float) r, (float) g, (float) b);
			glBegin(GL_QUADS);
			glVertex2d(leftBound, 0);
			glVertex2d(rightBound, 0);
			glVertex2d(rightBound, SCREEN_HEIGHT);
			glVertex2d(leftBound, SCREEN_HEIGHT);
			glEnd();
			;
			glBegin(GL_LINES);

			if (getLeftStrength() == getRightStrength()) {
				glColor3f((RIGHTCOLOR[0] + LEFTCOLOR[0]) / 2,
						(RIGHTCOLOR[1] + LEFTCOLOR[1]) / 2,
						(RIGHTCOLOR[2] + LEFTCOLOR[2]) / 2);
				glVertex2d(leftBound, getScreenLSPos());
				glVertex2d(rightBound, getScreenLSPos());
			} else {
				glColor3f(LEFTCOLOR[0], LEFTCOLOR[1], LEFTCOLOR[2]);
				glVertex2d(leftBound, getScreenLSPos());
				glVertex2d(rightBound, getScreenLSPos());

				glColor3f(RIGHTCOLOR[0], RIGHTCOLOR[1], RIGHTCOLOR[2]);
				glVertex2d(leftBound, getScreenRSPos());
				glVertex2d(rightBound, getScreenRSPos());

			}

			if (getLeftStrength() != 0 && getRightStrength() != 0) {
				glColor3f(MIXCOLOR[0], MIXCOLOR[1], MIXCOLOR[2]);
				glVertex2d(leftBound, getScreenLRSPos());
				glVertex2d(rightBound, getScreenLRSPos());
			}

			glEnd();
		}

		public String toString() {
			return "<Pill: LS(" + getLeftStrength() + ") RS("
					+ getRightStrength() + ")>";
		}

	}

	public static void main(String[] args) {
		new CounterWaveTest1();

	}

}
