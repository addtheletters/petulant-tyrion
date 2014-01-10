package atl.pressurewave;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class CounterWaveTest1 {
	
	/**
	 * Display colors
	 */
	final float LEFTCOLOR[] 	= { 1f, 0f, 0f };
	final float RIGHTCOLOR[] 	= { 0f, 0f, 1f };
	final float MIXCOLOR[] 		= {1f, 1f, 1f};
	final float NULLCOLOR[] 	= {.5f, .5f, .5f};
	
	/**
	 * Keybinds, listed in highest priority to lowest priority.
	 * If no modifiers are pressed, it will default to amplify plus.
	 * Used mostly in clickHelper().
	 */
	final int MOUSE_ENABLE_KEY 				= 	Keyboard.KEY_E;
	final int MOUSE_DISABLE_KEY 			= 	Keyboard.KEY_D;
	final int MODIFIER_AMPLIFY_PLUS_KEY 	= 	Keyboard.KEY_SPACE;
	final int MODIFIER_AMPLIFY_MINUS_KEY 	=	Keyboard.KEY_LSHIFT;
	final int MODIFIER_TIMESCALE_PLUS_KEY 	= 	Keyboard.KEY_UP;
	final int MODIFIER_TIMESCALE_MINUS_KEY 	= 	Keyboard.KEY_DOWN;
	final int MODIFIER_PHASESHIFT_PLUS_KEY 	=	Keyboard.KEY_RIGHT;
	final int MODIFIER_PHASESHIFT_MINUS_KEY = 	Keyboard.KEY_LEFT;
	
	
	ArrayList<Pillar> pills;
	final int NUM_PILLS 			= 500;		//number of nodes, lateral resolution
	final int TICK_DELAY 			= 0;		//True Tick Delay between lateral propagation ticks
	final double POWER_LIMIT 		= 10;		//Determines scaling of screen view
	final double THETA_STEP 		= 0.1;		//Increment to theta each update
	final double MOUSE_POWER 		= 1;		//Scales impact of mouse overall
	final double MOUSE_AMP_POWER	= 0.05;		//Mouse amplifier multiplier (one plus or minus)
	final double MOUSE_TS_POWER		= 0.05; 	//Mouse timescale multiplier (one plus or minus)
	final double MOUSE_PS_POWER		= 1; 		//Mouse phaseshift boost amount
	
	/**
	 * Modifiers for functions. Functions themselves are defined as leftFunction and rightFunction
	 * with a parameter of theta. Theta is pretty much time as it is constantly incremented
	 */
	double leftAmp 			= 1;
	double rightAmp 		= 2;
	double leftTimeScale 	= 0.5;
	double rightTimeScale 	= 0.1;
	double leftPhaseShift 	= 1;
	double rightPhaseShift 	= 0;
	
	/**	
	 * tick is used along with TICK_DELAY for propagation of values laterally.
	 * Theta is used to calculate function values and updates independently of
	 * lateral propagation (calculated every frame update)
	 */
	int tick 		= 0;
	double theta 	= 0.0;

	private boolean mouseEnabled = true;
	
	private final int FRAME_RATE_SYNC = 60;	//openGL will attempt to sync framerate to this

	public static final int SCREEN_WIDTH = 1280;
	public static final int SCREEN_HEIGHT = 790;

	private final String TITLE = "'Murican Wavefighting!";

	// private long lastFrame; // used if using delta to adjust speeed

	/*
	 * private long getTime() { return (Sys.getTime() * 1000) /
	 * Sys.getTimerResolution(); }
	 */

	/**
	 * Delta is a value used to track the framerate and adjust speed of
	 * interactions to ensure that things happen consistently across machines
	 * with different speeds. Not using it here.
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

	public CounterWaveTest1() {
		setUpDisplay();//.5
		setUpOpenGL();

		// setUpTimer();

		// int delta;

		setUpPillars();

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
		theta += THETA_STEP;
		//System.out.println("HIIIIIIII");

		sendSignals();
		
		if (tick > TICK_DELAY) {
			tick = 0;
			//displayPillarStatus();
			propagateLeft();
			propagateRight();
		}
		// tick world.
		return;
	}
	
	
	private void sendSignals(){
		sendLeftSignal();
		sendRightSignal();
	}

	
	private double leftFunction(double t){
		return Math.pow(2,Math.tan(Math.sin(Math.tan(t))));
	}
	
	private double rightFunction(double t){
		return -1.0/Math.cos(Math.pow(t,1));
	}
	
	
	private void sendLeftSignal(){
		Pillar p = pills.get(0);
		p.setLeftStrength( leftAmp * leftFunction((theta * leftTimeScale) + leftPhaseShift)  );
	}
	
	private void sendRightSignal(){
		Pillar p = pills.get(NUM_PILLS - 1);
		p.setRightStrength( rightAmp * rightFunction((theta * rightTimeScale) + rightPhaseShift) );
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
			glVertex2d((int) ( (i+.5) * (SCREEN_WIDTH * 1.0) / NUM_PILLS), pills.get(i).getScreenParameterPos(path));
		}
		
		glEnd();
	}

	private void clickHelper(boolean left){
		
		if(Keyboard.isKeyDown(MODIFIER_AMPLIFY_PLUS_KEY)){
			if(left) leftAmp *= MOUSE_POWER * (1+MOUSE_AMP_POWER);
			else rightAmp *= MOUSE_POWER * (1+MOUSE_AMP_POWER);
			return;
		}
		if(Keyboard.isKeyDown(MODIFIER_AMPLIFY_MINUS_KEY)){
			if(left) leftAmp *= MOUSE_POWER * (1-MOUSE_AMP_POWER);
			else rightAmp *= MOUSE_POWER * (1-MOUSE_AMP_POWER);
			return;
		}
		if(Keyboard.isKeyDown(MODIFIER_TIMESCALE_PLUS_KEY)){
			if(left) leftTimeScale *= MOUSE_POWER * (1+MOUSE_TS_POWER);
			else rightTimeScale *= MOUSE_POWER * (1+MOUSE_TS_POWER);
			return;
		}
		if(Keyboard.isKeyDown(MODIFIER_TIMESCALE_MINUS_KEY)){
			if(left) leftTimeScale *= MOUSE_POWER * (1-MOUSE_TS_POWER);
			else rightTimeScale *= MOUSE_POWER * (1-MOUSE_TS_POWER);
			return;
		}
		if(Keyboard.isKeyDown(MODIFIER_PHASESHIFT_PLUS_KEY)){
			if(left) leftPhaseShift += MOUSE_POWER * MOUSE_PS_POWER;
			else rightPhaseShift += MOUSE_POWER * MOUSE_PS_POWER;
			return;
		}
		if(Keyboard.isKeyDown(MODIFIER_PHASESHIFT_MINUS_KEY)){
			if(left) leftPhaseShift -= MOUSE_POWER * MOUSE_PS_POWER;
			else rightPhaseShift -= MOUSE_POWER * MOUSE_PS_POWER;
			return;
		}
		if(left) leftAmp += MOUSE_POWER;
		else rightAmp += MOUSE_POWER;
		return;
	}

	
	private void input() {
		if (mouseEnabled) {
			//int mouseX = Mouse.getX();// - WIDTH / 2;
			//int mouseY = Mouse.getY();// - HEIGHT / 2;
			if (Mouse.isButtonDown(0)) {
				clickHelper(true);
			}
			if (Mouse.isButtonDown(1)) {
				clickHelper(false);
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

		@SuppressWarnings("unused")
		public void renderPillar(int leftBound, int rightBound, double r,
				double g, double b) {
			// System.out.println("HADUHGF");b
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
