// TODO: 
// 1. More levels - done, maybe we need still more ?
// 2. Other fish's movement improvement
// 3. Options page - done
// 4. Water effect

package maach.game;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

public class GameController implements SensorEventListener, OnTouchListener
{

	// various variables for controlling the state of the game
	private static final int GAME_RUNNING   = 1;
	private static final int START_SCREEN   = 2;
	private static final int LEVEL_SCREEN   = 3;
	private static final int GAME_PAUSED    = 4;
	private static final int LEVEL_COMPLETE = 5;
	private static final int OPTION_SCREEN  = 6;
	public static final int CALIBRATE_TILT_SCREEN  = 7;
	
	public static final int INSTRUCTION_TILT_SCREEN  = 8;
	private static final int INSTRUCTION_EAT_SCREEN  = 9;
	private static final int INSTRUCTION_GOAL_SCREEN  = 10;
	
	// one of the above states
	private static int controllerState;
	
	public static final int N_LEVELS = 10;
	
	// reference to the current menu screen
	private static MenuScreen currentMenu;
	
	private static int currentLevel = 0;
	public static int maxLevel = 0;
	

	// reference to our various other class objects (static because we have only single copies of them and easier to access from button classess)
	public static TwoDRenderer renderer = null;
	public static MaachMaze activity = null;
	public static GameState   game   = null;
	private static LevelLoader loader = null;
	
	static int screenWidth, screenHeight, screenRotation;
	
	
	public static LevelData []levelData;
	
	// references to the menu screen objects
	private static LogoScreen logoScreen;
	private static LevelsScreen levelsScreen;
	private static OptionScreen optionScreen;
	public static CalibrateTiltScreen calibrateTiltScreen;
	
	private static LevelCompleteScreen levelCompleteScreen;
	private static LevelFailedScreen levelFailedScreen;
	private static GamePausedScreen gamePausedScreen;

	
	// references to instruction screen objects
	public static InstructionScreen instructionTiltMove;
	private static InstructionScreen instructionEat;
	private static InstructionScreen instructionGoal;
	
	
	// acceleration values from accelerometer (in m/s^2)
	private static float sensorAx = 0.0f, sensorAy = 0.0f, sensorAz = 0.0f;
	
	// star images are used in both level complete screen and level selection screen
	// so we create them in GameController's constructor and use them in both places
	public static int filledStarImageHandle; 
	public static int emptyStarImageHandle; 
	public static int levelsScreenStarImageHandle;
	
	
	GameController (MaachMaze act, TwoDRenderer rend, int width, int height, int rotation)
	{
		activity = act;
		renderer = rend;
		screenWidth = width;
		screenHeight = height;
		screenRotation = rotation;
		
		// show a message as the game loads
		showMessage("Loading, please wait...", Toast.LENGTH_SHORT);
		
		// initialize level data
		initLevelsData();
		
		// create one instance of each MenuScreen object
		createMenuScreens();
		
		// creating object of LevelLoader class
	    loader = new LevelLoader(act); 	
	    
	    // Create and initialize the gameState object
		game  = new GameState();	
		game.initialize(act, renderer, screenWidth, screenHeight);

		// start the logo/start screen
		showLogoScreen();

		// create the star images
		filledStarImageHandle = GameController.renderer.loadImage(R.drawable.star_filled);
		emptyStarImageHandle  = GameController.renderer.loadImage(R.drawable.star_empty);
		levelsScreenStarImageHandle = GameController.renderer.loadImage(R.drawable.star_filled_crown);
	}
	
	private void initLevelsData()
	{
		levelData = new LevelData[N_LEVELS];
		
		for(int i = 0; i < N_LEVELS; i++)
		{
			levelData[i] = new LevelData();
			levelData[i].starsWon = 0;
		}
		
		levelData[0].normalButtonImage = GameController.renderer.loadImage(R.drawable.button1a);
		levelData[0].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button1b);
		levelData[0].levelFile = R.raw.level1;
		levelData[0].levelBackground = 0;
		
		// eat 0 fish -> 1 star
		// eat 1 fish -> 5 star
		levelData[0].minFishForStars[1] = 0;
		levelData[0].minFishForStars[2] = 1;
		levelData[0].minFishForStars[3] = 1;
		levelData[0].minFishForStars[4] = 1;
		levelData[0].minFishForStars[5] = 1;
		
		
		levelData[1].normalButtonImage = GameController.renderer.loadImage(R.drawable.button2a);
		levelData[1].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button2b);
		levelData[1].levelFile = R.raw.level2;
		levelData[1].levelBackground = 1;

		// eat 0 fish -> 1 star
		// eat 1 fish -> 3 star
		// eat 2 fish -> 5 star		
		levelData[1].minFishForStars[1] = 0;
		levelData[1].minFishForStars[2] = 1;
		levelData[1].minFishForStars[3] = 1;
		levelData[1].minFishForStars[4] = 2;
		levelData[1].minFishForStars[5] = 2;
	
		
		levelData[2].normalButtonImage = GameController.renderer.loadImage(R.drawable.button3a);
		levelData[2].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button3b);
		levelData[2].levelFile = R.raw.level3;
		levelData[2].levelBackground = 2;
		
		levelData[2].minFishForStars[1] = 0;
		levelData[2].minFishForStars[2] = 1;
		levelData[2].minFishForStars[3] = 3;
		levelData[2].minFishForStars[4] = 5;
		levelData[2].minFishForStars[5] = 7;
	
		
		levelData[3].normalButtonImage = GameController.renderer.loadImage(R.drawable.button4a);
		levelData[3].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button4b);
		levelData[3].levelFile = R.raw.level4;
		levelData[3].levelBackground = 2;
		
		levelData[3].minFishForStars[1] = 0;
		levelData[3].minFishForStars[2] = 2;
		levelData[3].minFishForStars[3] = 4;
		levelData[3].minFishForStars[4] = 6;
		levelData[3].minFishForStars[5] = 8;
	
		
		levelData[4].normalButtonImage = GameController.renderer.loadImage(R.drawable.button5a);
		levelData[4].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button5b);
		levelData[4].levelFile = R.raw.level5;
		levelData[4].levelBackground = 3;
		
		levelData[4].minFishForStars[1] = 0;
		levelData[4].minFishForStars[2] = 1;
		levelData[4].minFishForStars[3] = 1;
		levelData[4].minFishForStars[4] = 2;
		levelData[4].minFishForStars[5] = 3;
	
		
		levelData[5].normalButtonImage = GameController.renderer.loadImage(R.drawable.button6a);
		levelData[5].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button6b);
		levelData[5].levelFile = R.raw.level6;
		levelData[5].levelBackground = 3;
		
		levelData[5].minFishForStars[1] = 0;
		levelData[5].minFishForStars[2] = 3;
		levelData[5].minFishForStars[3] = 6;
		levelData[5].minFishForStars[4] = 9;
		levelData[5].minFishForStars[5] = 11;
	
		
		levelData[6].normalButtonImage = GameController.renderer.loadImage(R.drawable.button7a);
		levelData[6].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button7b);
		levelData[6].levelFile = R.raw.level7;
		levelData[6].levelBackground = 1;
		
		levelData[6].minFishForStars[1] = 0;
		levelData[6].minFishForStars[2] = 4;
		levelData[6].minFishForStars[3] = 7;
		levelData[6].minFishForStars[4] = 10;
		levelData[6].minFishForStars[5] = 13;
	
		
		levelData[7].normalButtonImage = GameController.renderer.loadImage(R.drawable.button8a);
		levelData[7].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button8b);
		levelData[7].levelFile = R.raw.level8;
		levelData[7].levelBackground = 3;
		
		levelData[7].minFishForStars[1] = 0;
		levelData[7].minFishForStars[2] = 7;
		levelData[7].minFishForStars[3] = 15;
		levelData[7].minFishForStars[4] = 20;
		levelData[7].minFishForStars[5] = 25;
	
		
		levelData[8].normalButtonImage = GameController.renderer.loadImage(R.drawable.button9a);
		levelData[8].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button9b);
		levelData[8].levelFile = R.raw.level9;
		levelData[8].levelBackground = 3;
		
		levelData[8].minFishForStars[1] = 0;
		levelData[8].minFishForStars[2] = 5;
		levelData[8].minFishForStars[3] = 10;
		levelData[8].minFishForStars[4] = 15;
		levelData[8].minFishForStars[5] = 19;
	

		levelData[9].normalButtonImage = GameController.renderer.loadImage(R.drawable.button10a);
		levelData[9].pressedButtonImage = GameController.renderer.loadImage(R.drawable.button10b);
		levelData[9].levelFile = R.raw.level10;
		levelData[9].levelBackground = 2;
		
		levelData[9].minFishForStars[1] = 0;
		levelData[9].minFishForStars[2] = 5;
		levelData[9].minFishForStars[3] = 10;
		levelData[9].minFishForStars[4] = 15;
		levelData[9].minFishForStars[5] = 20;
		
		// populate the maxLevel and starts won information from file
		activity.readLevelDataFromFile();
	}
	
	private void createMenuScreens() {
		logoScreen = new LogoScreen();
		levelsScreen = new LevelsScreen();
		optionScreen = new OptionScreen();
		calibrateTiltScreen = new CalibrateTiltScreen();
		
		instructionTiltMove = new InstructionScreen(R.drawable.instruction1);
		instructionEat = new InstructionScreen(R.drawable.instruction2);
		instructionGoal = new InstructionScreen(R.drawable.instruction3);
		
		levelCompleteScreen = new LevelCompleteScreen();
		levelFailedScreen = new LevelFailedScreen();
		gamePausedScreen = new GamePausedScreen();
	}
	
	void updatePlayerFishAcceleration(float ax, float ay)
	{
		// put the acceleration values we got from accelerometer or touch in player's fish object
		game.myFish.get(0).ax = ax;
		game.myFish.get(0).ay = ay;		
	}
	
	
	
	// Accelerometer handling
	
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// we don't care about this
	}

	public void onSensorChanged(SensorEvent sensorEvent) {
		if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
		{
			float ax=0, ay=0, az=0;				
			/*
			// HACK: we run our program in landscape mode, so we want the
			// horizontal landscape direction (the longer side of the
			// screen) to be the x - axis
			// but phone's co-ordiate system has smaller side as x-axis and
			// bigger side as y axis.
			// so, we just get x-direction acceleration in ay, and y
			// direction acceleration in ax
			
			ay = sensorEvent.values[0]; 
			ax = sensorEvent.values[1]; 
			*/
			
			az = sensorEvent.values[2];

			
			// figure out correct ax, ay depending on orientation
			// croxx fingers and hope this will work on all devices
			// see http://android-developers.blogspot.in/2010/09/one-screen-turn-deserves-another.html for details
			switch(screenRotation)
			{
                case Surface.ROTATION_0:
                    ax = -sensorEvent.values[0];
                    ay = sensorEvent.values[1];
                    break;
                default:
                    ax = sensorEvent.values[1];
                    ay = sensorEvent.values[0];
                    break;					
			}

			// low pass filter
			final float alpha = 0.5f;
			sensorAx = alpha * sensorAx + (1 - alpha) * ax;
			sensorAy = alpha * sensorAy + (1 - alpha) * ay;
			sensorAz = alpha * sensorAz + (1 - alpha) * az;
			
			// update the gameState
			if (controllerState == GAME_RUNNING && (!Settings.useTouchInput))
			{
				
				final float MAX_ACCELERATION = 9.8f;
				
				ax = (sensorAx - Settings.neutral_Ax) * Settings.sensitivity;
				ay = (sensorAy - Settings.neutral_Ay) * Settings.sensitivity;
				
				float total_accel  = (float) Math.sqrt(ax * ax + ay * ay);
				float capped_accel = Math.min(total_accel, MAX_ACCELERATION);
				
				ax = ax * capped_accel / total_accel;
				ay = ay * capped_accel / total_accel;
				
				
				/*
				ax = Math.min(ax, MAX_ACCELERATION);
				ay = Math.min(ay, MAX_ACCELERATION);
				ax = Math.max(ax, -MAX_ACCELERATION);
				ay = Math.max(ay, -MAX_ACCELERATION);
				*/
				updatePlayerFishAcceleration(ax, ay);
			}
						
		}

	}

	
	
	// touch handling
	static final float TOUCH_ACC_SCALE = 0.02f;
	
	public int lastTouchX, lastTouchY; 
	
    public boolean onTouch(View v, MotionEvent event) 
	{
		float touchX = event.getRawX();
		float touchY = event.getRawY();
		
		if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
		{
			if (controllerState == GAME_RUNNING)
			{
				if (Settings.useTouchInput)
				{
					// handle touch acceleration
					float fishScreenX = game.myFish.get(0).x - game.screenX;
					float fishScreenY = game.myFish.get(0).y - game.screenY;
					
					
					float ax = (touchX - fishScreenX) * TOUCH_ACC_SCALE;
					float ay = (touchY - fishScreenY) * TOUCH_ACC_SCALE;
					
					updatePlayerFishAcceleration(ax, ay);
				}
			}
			else
			{
				currentMenu.onTouchDownOrMove( (int)touchX, (int)touchY);				
			}
		}
		else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
		{
			if (controllerState == GAME_RUNNING)
			{			
				if (Settings.useTouchInput)
				{
					// stop accelerating when no longer touching
					updatePlayerFishAcceleration(0, 0);	
				}
			}
			else
			{
				currentMenu.onTouchUp( (int)touchX, (int)touchY);
			}
		}
		

		return true;
	}
    
	public static void exitGame() 
	{
		activity.finish();
	}

	public static void showLevelsScreen() 
	{
		controllerState = LEVEL_SCREEN;		
		renderer.setFrameLooper(levelsScreen);
		currentMenu = levelsScreen;
		GameState.themeSong.start();		
	}

	public static void showOptionsScreen() 
	{
		
		controllerState = OPTION_SCREEN;
		renderer.setFrameLooper(optionScreen);
		currentMenu = optionScreen;
			
	}
	
	public static void showMenuScreen(MenuScreen screen, int newControllerState)
	{
		controllerState = newControllerState;
		renderer.setFrameLooper(screen);
		currentMenu = screen;
	}
	
	public static void loadGame(int levelFile, int levelNumber, int levelBackground)
	{
		if(levelNumber > maxLevel)
		{
			showMessage("Complete all previous levels to unlock this level", Toast.LENGTH_SHORT);
			return;
		}

		
		game.loadLevel(loader, levelFile, levelBackground);
		
		// start the game (by making it the current screen)
		renderer.setFrameLooper(game);
		controllerState = GAME_RUNNING;	
		currentLevel = levelNumber;
		currentMenu = null;
		
		GameState.themeSong.pause();
		GameState.backgroundMusic.start();		
	}
	
	public static void loadGame(int levelNumber)
	{
		
		loadGame(levelData[levelNumber].levelFile, levelNumber, levelData[levelNumber].levelBackground);
	}
	
	// hack: we want to synchronize rendering (espicially drawing of the game background) and the new game loading operation
	// so we don't load the level directly here but instead at the end of render() of LevelComplete/Failed screens.
	// non-zero value indicates that a level load is pending.
	
	public static int levelLoadPendingLevelNumber = 0;
	
	public static void loadNextGameLevel()
	{
		if (currentLevel + 1 < N_LEVELS)
			levelLoadPendingLevelNumber = currentLevel + 1;
		else
		{
			showMessage("You have finished all levels! Now try completing every level with 5 stars, or send suggestions/feedback to ankan_ban@yahoo.com :-)", Toast.LENGTH_LONG);
			showLevelsScreen();
		}
	}
	
	public static void resetCurrentGameLevel()
	{
		assert(currentLevel <= maxLevel);
		levelLoadPendingLevelNumber = currentLevel;		
	}
	
	public static void showLogoScreen()
	{
		GameState.themeSong.start();
		controllerState = START_SCREEN;
		renderer.setFrameLooper(logoScreen);
		currentMenu = logoScreen;
	}

	public static void onBackPressed() 
	{
		if(controllerState == LEVEL_SCREEN)
		{
			showLogoScreen();
		}
		else if (controllerState == INSTRUCTION_TILT_SCREEN)
		{
			showLogoScreen();
		}
		else if (controllerState == INSTRUCTION_EAT_SCREEN)
		{
			showMenuScreen(instructionTiltMove, INSTRUCTION_TILT_SCREEN);
		}
		else if (controllerState == INSTRUCTION_GOAL_SCREEN)
		{
			showMenuScreen(instructionEat, INSTRUCTION_EAT_SCREEN);			
		}
		else if (controllerState == LEVEL_COMPLETE)
		{
			showLevelsScreen();
		}
		else if (controllerState == GAME_RUNNING)
		{
			game.paused = true;
			showMenuScreen(gamePausedScreen, GAME_PAUSED);
		}
		else if(controllerState == GAME_PAUSED)
		{
			ResumeGame();
		}
		else if (controllerState == OPTION_SCREEN)
		{
			showLogoScreen();			
		}
		else if (controllerState == CALIBRATE_TILT_SCREEN)
		{
			GameController.showOptionsScreen();
		}
	}

	static int computeStars(int noFishEaten, int level)
	{
		for(int i = 5; i > 0; i--)
		{
			if(noFishEaten >= levelData[level].minFishForStars[i])
			{
				return i;
			}		
		}
		return 0;
	}
	
	public static void gameFinished(boolean levelComplete, int noOfFishEaten) 
	{
		GameState.backgroundMusic.pause();
		if(levelComplete)
		{		
			GameState.victory.start();
			
			// compute no. of stars won this time
			int nStars = computeStars(noOfFishEaten, currentLevel);
			
			// update levelData if this is a best score
			if (nStars > levelData[currentLevel].starsWon)
				levelData[currentLevel].starsWon = nStars;
			
			// update the no. of stars to show in the level complete screen
			LevelCompleteScreen.nStarsToShow = nStars;

			if (maxLevel < N_LEVELS - 1)
			{
				maxLevel = Math.max(maxLevel, currentLevel + 1);
			}
			showMenuScreen(levelCompleteScreen, LEVEL_COMPLETE);
		}
		else
		{
			GameState.loser.start();
			showMenuScreen(levelFailedScreen, LEVEL_COMPLETE);
		}
	}

	public static void handleInstructionScreensNextButtonPress()
	{
		if (controllerState == INSTRUCTION_TILT_SCREEN)
			showMenuScreen (instructionEat, INSTRUCTION_EAT_SCREEN);
		else if (controllerState == INSTRUCTION_EAT_SCREEN)
		{
			showMenuScreen (instructionGoal, INSTRUCTION_GOAL_SCREEN);
		}
		else if (controllerState == INSTRUCTION_GOAL_SCREEN)
		{
			showLevelsScreen();			
		}
	}

	public static void ResumeGame() 
	{
		game.paused = false;

		renderer.setFrameLooper(game);
		controllerState = GAME_RUNNING;	
		currentMenu = null;		
	}
	
	public static void soundsOn()
	{
		Settings.soundsOn = true;
		
		// enable the sounds
		GameState.crunchBig.setVolume(1.0f, 1.0f);
		GameState.crunchSmall.setVolume(1.0f, 1.0f);
		GameState.themeSong.setVolume(1.0f, 1.0f);
		GameState.victory.setVolume(1.0f, 1.0f);
		GameState.loser.setVolume(1.0f, 1.0f);
		GameState.backgroundMusic.setVolume(0.2f, 0.2f);			
	}
	
	public static void soundsOff()
	{
		Settings.soundsOn = false;	
		
		// disable the sounds
		GameState.crunchBig.setVolume(0, 0);
		GameState.crunchSmall.setVolume(0, 0);
		GameState.themeSong.setVolume(0, 0);
		GameState.victory.setVolume(0, 0);
		GameState.loser.setVolume(0, 0);		
		GameState.backgroundMusic.setVolume(0, 0);		
	}
	
	
	public static void calibrateTilt()
	{
		Settings.neutral_Ax = sensorAx;
		Settings.neutral_Ay = sensorAy;
		showMessage("Neutral position changed to ax = " + sensorAx + ", ay = " + sensorAy, Toast.LENGTH_SHORT);
	}
	
	public static void showMessage(String message, int duration)
	{
		Context context = GameController.activity; 
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();	
	}
	
	public static void showOKDialog(String message)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage(message)
		       .setCancelable(true)
		       .setNegativeButton("OK", new OkButtonClick());
		AlertDialog alert = builder.create();
		alert.show();
	}
}

class OkButtonClick implements DialogInterface.OnClickListener
{
    public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
   }	
}

// class representing any interactive screen with buttons that can be clicked - including the start/logo screen
abstract class MenuScreen implements GameFrameLoop
{
	protected int backgroundImage;
	protected boolean wavyBackground;
	
	// in /WIDTH_UNITS, /HEIGHT_UNITS
	protected float width, height;
	
	protected float alpha;
	
	MenuScreen()
	{
		// default is to cover the entire screen, but some kind of menus (dialog box) 
		// might want to override this
		width  = WIDTH_UNITS; 
		height = HEIGHT_UNITS;
		
		// default is opaque background, subclasses might override
		alpha = 1.0f;
	}
	
	// all buttons (interactive elements) on this screen
	protected ArrayList<Button> buttons;
	
	// either touched or touched-and-moved (dragged/swiped)
	public void onTouchDownOrMove(int x, int y)
	{
		int n = buttons.size();
		for (int i = 0; i < n; i++)
		{
			if (buttons.get(i).isInsideButton(x, y))
				buttons.get(i).press();
			else
				buttons.get(i).release();
		}
	}
		
	public void onTouchUp(int x, int y)
	{
		int n = buttons.size();
		for (int i = 0; i < n; i++)
		{
			Button button = buttons.get(i); 
			if (button.isInsideButton(x, y))
			{
				if (button.isPressed())
				{
					button.onClicked();
				}
			}
			button.release();
		}		
	}
	
	
	// basically fixes an aspect ratio of the screen (defaulted to Half HD 720p aspect ratio)
	public static final float WIDTH_UNITS = 1280.0f;
	public static final float HEIGHT_UNITS = 720.0f;
	
	public static void drawImageRelativeCoords(float rx, float ry, float rwidth, float rheight, int imageHandle, TwoDRenderer renderer)
	{
		float x 	 = rx * GameController.screenWidth / WIDTH_UNITS;
		float y 	 = ry * GameController.screenHeight / HEIGHT_UNITS;
		float width	 = rwidth * GameController.screenWidth / WIDTH_UNITS;
		float height = rheight * GameController.screenHeight / HEIGHT_UNITS;
				
		renderer.drawImageScaled(imageHandle, x + width/2, y + height/2,  width, height, 0);
	}
	
	
	// for fps counting
	private static final int framesAvg = 20;
	private int  frameNumber = 0;
	private long lastSavedTime = 0;

	
	// draw the menu screen
	public void render(TwoDRenderer renderer) 
	{
		
		// For FPS counting : start
		long currentTime = System.nanoTime();
		frameNumber++;
		if(frameNumber % framesAvg == 0)
		{
			Log.v("maach", "fps: " + (framesAvg * 1000000000.0 / (currentTime - lastSavedTime)));
			lastSavedTime = currentTime;
		}
		// For FPS counting : end			

		// draw the background
		
		// figure out the width/height in pixels
		float backWidth = width * GameController.screenWidth / WIDTH_UNITS;
		float backHeight = height * GameController.screenHeight / HEIGHT_UNITS;
		
		if(wavyBackground)
		{
			
			renderer.drawWavyImageScaled(backgroundImage, GameController.screenWidth/2, GameController.screenHeight/2, 
										 backWidth , backHeight, 0, System.nanoTime());
		}
		else
		{
			renderer.drawImageScaledTransparent(backgroundImage, GameController.screenWidth/2, GameController.screenHeight/2, 
									 			backWidth, backHeight, 0, alpha);
		}
		
		// draw the buttons
		for (int i = 0; i < buttons.size(); i++)
		{
			buttons.get(i).draw(renderer);
		}
		
		/*
		// for testing - seeing individual frames
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
	}
}

// a button on any interactive screen
abstract class Button
{
	
	// All co-ordinates and sizes are 
	// in units of (real screen width or height) / (WIDTH_UNITS or HEIGHT_UNITS)  pixels	
	protected float x, y;	
	protected float width, height;

	// the state of the button
	protected boolean pressed = false;	
	
	// images for pressed and normal states of the button
	protected int normalImageHandle;
	protected int pressedImageHandle;

	
	public void press()
	{
		pressed = true;
		
	}
	
	public void release()
	{
		pressed = false;
	}
	
	public boolean isPressed()
	{
		return pressed;
	}
	
	public boolean isInsideButton(int screenX, int screenY)
	{
		// convert screen co-ordinates to our co-ordinates
		float x = screenX * MenuScreen.WIDTH_UNITS / GameController.screenWidth;
		float y = screenY * MenuScreen.HEIGHT_UNITS / GameController.screenHeight;
				
		if (x >= this.x && x <= this.x + width &&
			y >= this.y && y <= this.y + height)
		{
			return true;
		}
		return false;
	}
	
	
	public void draw(TwoDRenderer renderer)
	{
		// draw the button using the given renderer
		
		int imageHandle;
		
		
		if(pressed) 
			imageHandle = pressedImageHandle;
		else
			imageHandle = normalImageHandle;
		
		MenuScreen.drawImageRelativeCoords(this.x, this.y, this.width, this.height, imageHandle, renderer);
	}

	// To be implemented by child classes
	public abstract void onClicked();
	
	
}

// A selection button is like a checkbox, it can be selected, pressed or deselected
// so it needs three images
abstract class SelectionButton extends Button
{
	// the selected state of button
	protected boolean selected = false;	
	
	// the image to use when the button is selected
	protected int selectedImageHandle;
		
	public void draw(TwoDRenderer renderer)
	{
		// draw the button using the given renderer
		
		int imageHandle;
		
		if(pressed)
		{
			imageHandle = pressedImageHandle;
		}
		else
		{       
			if(selected)
				imageHandle = selectedImageHandle;
			else
				imageHandle = normalImageHandle;
		}
		
		MenuScreen.drawImageRelativeCoords(this.x, this.y, this.width, this.height, imageHandle, renderer);
	}	
}


// --- LOGO Screen --- //
///////////////////////////////
//         maach.game        //      
//                           //
//   exit   options   start  //
///////////////////////////////


class LogoScreen extends MenuScreen
{
	StartButton startButton;
	ExitButton  exitButton;
	OptionButton optionButton;
	
	public LogoScreen()
	{
		startButton = new StartButton();
		exitButton = new ExitButton();
		optionButton = new OptionButton();
		
		
		
		buttons = new ArrayList<Button>(3);
		buttons.add(startButton);
		buttons.add(exitButton);
		buttons.add(optionButton);

		backgroundImage = GameController.renderer.loadImage(R.drawable.firstscreen);
		wavyBackground = true;
		
	}
}



class StartButton extends Button
{

	public StartButton()
	{
		y = 570;
		x = 920;
		width = 300;
		height = 150;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.play);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.play_pressed);
	}
	
	public void onClicked() 
	{
		if (GameController.levelData[0].starsWon == 0)
		{
			GameController.showMenuScreen (GameController.instructionTiltMove, GameController.INSTRUCTION_TILT_SCREEN );
		}
		else
		{
			GameController.showLevelsScreen();
		}
	}	
}

class ExitButton extends Button
{

	public ExitButton()
	{
		y = 570;
		x = 90;
		width = 300;
		height = 150;		
		normalImageHandle = GameController.renderer.loadImage(R.drawable.quit);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.quit_pressed);
	}
	
	public void onClicked() 
	{
		// exit the game
		GameController.exitGame();
		GameState.themeSong.stop();
	}	
}

class OptionButton extends Button
{

	public OptionButton()
	{
		y = 570;
		x = 500;
		width = 300;
		height = 150;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.options);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.options_pressed);
		
	}
	
	public void onClicked() 
	{
		GameController.showOptionsScreen();
	}	
}



//--- Options Screen --- //
/////////////////////////////////////////////////
//                                             //
//  Game input method:    * tilt               //      
//           		      * touch              //
//                                             //
//  Sounds   :            * ON  OFF            //
//                                             //
//  Water Effect :        * ON OFF             //
//                                             //
//  Renderer:             * OpenGL             //
//                        * Canvas             //
//                                             //
/////////////////////////////////////////////////


class OptionScreen extends MenuScreen
{
	TiltModeButton tiltModeButton;
	TouchModeButton touchModeButton;
	OpenglButton openglButton;
	CanvasButton canvasButton;
	SoundsOnButton soundsOnButton;
	SoundsOffButton soundsOffButton;
	BackFromOptionsButton backFromOptionsButton;
	HelpButton helpButton;

	CreditsButton creditsButton;
	CalibrateTiltButton calibrateTiltButton;
	ResetButton resetButton;
	WaterOnButton waterOnButton;
	WaterOffButton waterOffButton;
	
	
	public OptionScreen()
	{
		tiltModeButton = new TiltModeButton();
		touchModeButton = new TouchModeButton();
		openglButton = new OpenglButton();
		canvasButton = new CanvasButton();
		soundsOnButton = new SoundsOnButton();
		soundsOffButton = new SoundsOffButton();
		backFromOptionsButton = new BackFromOptionsButton();
		helpButton = new HelpButton();
		
		creditsButton = new CreditsButton();
		calibrateTiltButton = new CalibrateTiltButton();
		resetButton = new ResetButton();
		waterOnButton = new WaterOnButton();
		waterOffButton = new WaterOffButton();
			
		buttons = new ArrayList<Button>(13);
		buttons.add(tiltModeButton);
		buttons.add(touchModeButton);
		buttons.add(openglButton);
		buttons.add(canvasButton);
		buttons.add(soundsOnButton);
		buttons.add(soundsOffButton);
		buttons.add(backFromOptionsButton);
		buttons.add(helpButton);		
		buttons.add(creditsButton);
		buttons.add(calibrateTiltButton);
		buttons.add(resetButton);
		buttons.add(waterOnButton);
		buttons.add(waterOffButton);
	

		backgroundImage = GameController.renderer.loadImage(R.drawable.options_screen);
		wavyBackground = false;
		
	}			
	
}

class TiltModeButton extends SelectionButton
{
	public TiltModeButton()
	{
		y = 110;
		x = 380;
		width = 200;
		height = 100;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.tilt_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.tilt_pressed);
		selectedImageHandle = GameController.renderer.loadImage(R.drawable.tilt_selected);		
	}

	public void onClicked()
	{	
		if (Settings.useTouchInput)
		{
			GameController.showMessage("Tilt mode selected. Tilt your phone/tablet to control your fish.", Toast.LENGTH_LONG);			
			Settings.useTouchInput = false;
		}
	}
	
	public void draw(TwoDRenderer renderer)
	{
		selected = !Settings.useTouchInput;
		super.draw(renderer);
	}
	
}

class TouchModeButton extends SelectionButton
{
	public TouchModeButton()
	{
		y = 110;
		x = 630;
		width = 200;
		height = 100;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.touch_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.touch_pressed);
		selectedImageHandle = GameController.renderer.loadImage(R.drawable.touch_selected);
	}

	public void onClicked()
	{
		if (!Settings.useTouchInput)
		{
			GameController.showMessage("Touch input mode selected.\nTouch the screen anywhere to move your fish.\nThe fish will try to follow your finger", Toast.LENGTH_LONG);
		}
		Settings.useTouchInput = true;	
	}	
	public void draw(TwoDRenderer renderer)
	{
		selected = Settings.useTouchInput;
		super.draw(renderer);
	}
}
class OpenglButton extends SelectionButton
{
	public OpenglButton()
	{
		y = 440;
		x = 380;
		width = 200;
		height = 100;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.opengl_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.opengl_pressed);
		selectedImageHandle = GameController.renderer.loadImage(R.drawable.opengl_selected);
	}

	public void onClicked()
	{
		if (Settings.hasOpenGLSupport)
		{
			if (Settings.useOpengl == false)
			{
				GameController.showMessage("Rendering Mode will be changed after an app-restart.", Toast.LENGTH_SHORT);
			}
			Settings.useOpengl = true;			
		}
		else
		{
			GameController.showMessage("Your phone doesn't support OpenGL ES 2.0\nGet a new phone to fully experience this game :)", Toast.LENGTH_LONG);			
		}
	}	
	public void draw(TwoDRenderer renderer)
	{
		selected = Settings.useOpengl;
		
		super.draw(renderer);		
	}
}
class CanvasButton extends SelectionButton
{
	public CanvasButton()
	{
		y = 440;
		x = 630;
		width = 200;
		height = 100;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.canvas_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.canvas_pressed);
		selectedImageHandle = GameController.renderer.loadImage(R.drawable.canvas_selected);
	}

	public void onClicked()
	{
		if (Settings.useOpengl == true)
		{
			GameController.showMessage("Rendering Mode will be changed after an app-restart.", Toast.LENGTH_SHORT);
		}		
		Settings.useOpengl = false;		
	}	
	
	public void draw(TwoDRenderer renderer)
	{
		selected = !Settings.useOpengl;
		super.draw(renderer);		
	}
}

class SoundsOnButton extends SelectionButton
{
	public SoundsOnButton()
	{
		y = 220;
		x = 380;
		width = 200;
		height = 100;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.on_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.on_pressed);
		selectedImageHandle = GameController.renderer.loadImage(R.drawable.on_selected);
	}
	
	public void onClicked()
	{
		GameController.soundsOn();
	}	
	
	public void draw(TwoDRenderer renderer)
	{
		selected = Settings.soundsOn;
		super.draw(renderer);		
	}
}

class SoundsOffButton extends SelectionButton
{
	public SoundsOffButton()
	{
		y = 220;
		x = 630;
		width = 200;
		height = 100;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.off_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.off_pressed);
		selectedImageHandle = GameController.renderer.loadImage(R.drawable.off_selected);
	}

	public void onClicked()
	{
		GameController.soundsOff();
	}	
	public void draw(TwoDRenderer renderer)
	{
		selected = !Settings.soundsOn;
		
		super.draw(renderer);		
	}
}

class WaterOnButton extends SelectionButton
{
	public WaterOnButton()
	{
		y = 330;
		x = 380;
		width = 200;
		height = 100;
		// TODO, important for memory/perf: create the on/off buttons at some common place and use the same handles instead of repeating
		normalImageHandle = GameController.renderer.loadImage(R.drawable.on_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.on_pressed);
		selectedImageHandle = GameController.renderer.loadImage(R.drawable.on_selected);
	}
	
	public void onClicked()
	{
		if (Settings.useOpengl)
		{
			if (Settings.waterOn == false)
			{
				GameController.showMessage("Water effect turned on.", Toast.LENGTH_SHORT);
			}
			Settings.waterOn = true;
		}
		else
		{
			GameController.showMessage("Water effect supported only in OpenGL mode", Toast.LENGTH_SHORT);			
		}
	}	
	
	public void draw(TwoDRenderer renderer)
	{
		selected = Settings.waterOn && Settings.useOpengl;
		super.draw(renderer);		
	}
}

class WaterOffButton extends SelectionButton
{
	public WaterOffButton()
	{
		y = 330;
		x = 630;
		width = 200;
		height = 100;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.off_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.off_pressed);
		selectedImageHandle = GameController.renderer.loadImage(R.drawable.off_selected);
	}
	
	public void onClicked()
	{
		if (Settings.waterOn == true)
		{
			GameController.showMessage("Water effect turned off.", Toast.LENGTH_SHORT);
		}		
		Settings.waterOn = false;
	}	
	
	public void draw(TwoDRenderer renderer)
	{
		selected = !(Settings.waterOn && Settings.useOpengl);
		super.draw(renderer);		
	}
}


class BackFromOptionsButton extends Button
{
	public BackFromOptionsButton()
	{
		y = 580;
		x = 150;
		width = 240;
		height = 120;	
		normalImageHandle = GameController.renderer.loadImage(R.drawable.back_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.back_pressed);	
	}

	public void onClicked()
	{
		GameController.showLogoScreen();
	}	
}

class HelpButton extends Button
{
	public HelpButton()
	{
		y = 580;
		x = 500;
		width = 240;
		height = 120;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.help_simple);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.help_pressed);
	}

	public void onClicked()
	{
		GameController.showMenuScreen (GameController.instructionTiltMove, GameController.INSTRUCTION_TILT_SCREEN );		
	}	
}

class CreditsButton extends Button
{
	public CreditsButton()
	{
		y = 580;
		x = 850;
		
		width = 240;
		height = 120;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.credits);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.credits_pressed);
	}

	public void onClicked()
	{
		GameController.showOKDialog("Developed by:\nAnubha: Concept, Art and Programming.\nAnkan: Level Design and Programming.\n\nSound and Image Credits: Kevin MacLeod, Mike Koenig, Monroe C. Fernandez, Hawksmont, sevenoaksart.co.uk, soundjay.com, freesfx.co.uk, gifs-paradise.com, Anubha");
	}	
}

class ResetButton extends Button
{
	public ResetButton()
	{
		y = 400;
		x = 980;
		
		width = 240;
		height = 120;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.reset);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.reset_pressed);
	}

	public void onClicked()
	{
		
		AlertDialog.Builder builder = new AlertDialog.Builder(GameController.activity);
		builder.setMessage("This will reset all settings to defaults and delete all high score/level completion data. Are you sure?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   
			       		GameController.soundsOn();
			       		if(Settings.hasOpenGLSupport)
			       			Settings.useOpengl = true;
			       		else
			       			Settings.useOpengl = false;
			       		
			    		Settings.useTouchInput = false;
			    		Settings.waterOn = false;
			    		Settings.sensitivity = 2.0f;
			    		Settings.neutral_Ax = Settings.neutral_Ay = 0.0f;
			    		
			    		GameController.maxLevel = 0;
			    		for(int i=0;i < GameController.N_LEVELS; i++)
			    		{
			    			GameController.levelData[i].starsWon = 0;
			    		}
			    		
			    		GameController.showMessage("All settings reset to defaults.\nHigh score data deleted.", Toast.LENGTH_SHORT);		
		           }
		       })
		       .setNegativeButton("No", new OkButtonClick());
		AlertDialog alert = builder.create();
		
		alert.show();
	}	
}

class CalibrateTiltButton extends Button
{
	public CalibrateTiltButton()
	{
		y = 120;
		x = 980;
		width = 256;
		height = 256 * 3 / 4.0f;
		normalImageHandle = GameController.renderer.loadImage(R.drawable.calibrate_tilt);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.calibrate_tilt_pressed);
	}

	public void onClicked()
	{			
		GameController.showMenuScreen (GameController.calibrateTiltScreen, GameController.CALIBRATE_TILT_SCREEN);		
	}	
}


//--- Instruction Screen --- //
////////////////////////////////////////////////////
//                                                //
//          <instructions in the background>      //
//                                                //
//                                        >> Next //
////////////////////////////////////////////////////
class InstructionScreen extends MenuScreen
{		
	
	NextButton nextButton;
	public InstructionScreen(int background)
	{
		nextButton = new NextButton();
		
		buttons = new ArrayList<Button>(1);
		buttons.add(nextButton);

		backgroundImage = GameController.renderer.loadImage(background);
		wavyBackground = false;
	}	
	
}

class NextButton extends Button
{
	public NextButton()
	{
		width = 144;
		height = 144;
		
		x = 1120;
		y = 560;		
		
		normalImageHandle = GameController.renderer.loadImage(R.drawable.nextbutton);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.nextbuttonpressed);		
	}
	
	public void onClicked() 
	{	
		GameController.handleInstructionScreensNextButtonPress();
	}	
}

class LevelData
{
	int normalButtonImage;
	int pressedButtonImage;	
	
	int levelFile;
	int levelBackground;
	
	// min no. of fishes the player should eat to earn stars
	int []minFishForStars = new int[6];
	int starsWon;
}


//--- Level Selection Screen --- //
///////////////////////////////////////////////////
//   1    2     3     4     5     6     7    8   //
//   9   10    11    12    13    14    15   16   //
//  17   18    19    20    21    22    23   24   //
//                                               //
//  Back                                 Exit    //
///////////////////////////////////////////////////

class LevelsScreen extends MenuScreen
{
	// TODO: support more than 1 page full of levels
	
	public static final int levelButtonsStartX = 100; 
	public static final int levelButtonsStartY = 160;
	public static final int levelButtonsGapX = 220;
	public static final int levelButtonsGapY = 250;
	private int button_x, button_y;
	
	// Circular level buttons
	public static final int LevelButtonRadius = 192/2;
	

	private static final int starSize = 32;
	
	public static final int levelsPerRow = 5;
	public static final int levelsPerColumn = 2;
	public static final int nLevelsPerScreen = levelsPerRow * levelsPerColumn;
	public static int nStarsToShow = 0;	
	

	public LevelsScreen()
	{	
		buttons = new ArrayList<Button>(nLevelsPerScreen);
	
		// 5x2 grid of level buttons

		for(int j = 0; j < levelsPerColumn; j++)
		{
			for (int i = 0; i < levelsPerRow; i++)
			{
				int buttonIndex = j * levelsPerRow + i;
				LevelButton button = new LevelButton();
				button_x = levelButtonsStartX + levelButtonsGapX * i;
				button_y = levelButtonsStartY + levelButtonsGapY * j;
				
				button.setPosition(button_x, button_y);			
			
				button.level = GameController.levelData[buttonIndex];
				button.setImageHandles(button.level.normalButtonImage, button.level.pressedButtonImage);
				
				button.levelNumber = buttonIndex;
				buttons.add(button);
			}
		}
		backgroundImage = GameController.renderer.loadImage(R.drawable.levelscreen);
		wavyBackground = false;
	}	
	
	void drawStars(float level_x, float level_y, int stars, TwoDRenderer renderer)
	{				
		// 15 degree in radians (configurable)
		double angularGap = 15 * Math.PI / 180;					

		float x, y;		
		double theta;
		
		float center_x = level_x + LevelButtonRadius;
		float center_y = level_y + LevelButtonRadius;

		int radius = LevelButtonRadius + 5;
		
		theta = Math.PI/2.0 - (stars - 1) * angularGap / 2.0;
		
		for (int i = 0; i < stars; i++)
		{
			x = center_x + (float) (radius * Math.cos(theta));
			y = center_y - (float) (radius * Math.sin(theta));
			
			MenuScreen.drawImageRelativeCoords(x - starSize/2, y - starSize/2, starSize, starSize, GameController.levelsScreenStarImageHandle, renderer);
		
			theta = theta + angularGap;
		}
				
	}
	
	public void render(TwoDRenderer renderer)
	{	
		super.render(renderer);
		
		for(int j = 0; j < levelsPerColumn; j++)
		{
			for (int i = 0; i < levelsPerRow; i++)
			{
				int buttonIndex = j * levelsPerRow + i;
	
				button_x = levelButtonsStartX + levelButtonsGapX * i;
				button_y = levelButtonsStartY + levelButtonsGapY * j;
				
				nStarsToShow = GameController.levelData[buttonIndex].starsWon;
				drawStars(button_x, button_y, nStarsToShow, renderer);							
			}
			
		}
	}
	
}


class LevelButton extends Button
{
	// the level to load when this button is clicked
	LevelData level;
	/*
	public int levelFileHandle;
	*/
	public int levelNumber;	
	
	public LevelButton()
	{
		width = LevelsScreen.LevelButtonRadius * 2;
		height = LevelsScreen.LevelButtonRadius * 2;
		
	}

	public void setPosition(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void setImageHandles (int normal, int pressed)
	{
		normalImageHandle = normal;
		pressedImageHandle = pressed;
	}
	
	public void onClicked() 
	{
		GameController.loadGame(level.levelFile, levelNumber, level.levelBackground);		
	}	
}

// also shows the game in background
class PopupScreen extends MenuScreen
{
	// draw the screen
	public void render(TwoDRenderer renderer) 
	{
		// first draw the game screen as the background
		GameController.game.render(renderer);
		
		super.render(renderer);
		
		if(GameController.levelLoadPendingLevelNumber > 0)
		{
			GameController.loadGame(GameController.levelLoadPendingLevelNumber);
			GameController.levelLoadPendingLevelNumber = 0;
		}
	}	
}

//--- Level Complete Screen --- //
// A dialog box kind of screen not covering whole screen
/////////////////////////
//   Level Complete    //
//     *****           //
//                     //
//  Quit       Next    //
/////////////////////////

class LevelCompleteScreen extends PopupScreen
{		
	NextLevelButton nextButton;
	ExitLevelButton exitButton;
	
	private static final int starY = 320;
	private static final int starStartX = 440;
	private static final int starSize = 72;
	private static final int starGap = 8;
		
	public static int nStarsToShow = 0;
	
	public LevelCompleteScreen()
	{
		
		nextButton = new NextLevelButton();
		exitButton = new ExitLevelButton();
		buttons = new ArrayList<Button>(2);
		buttons.add(nextButton);
		buttons.add(exitButton);
		
		backgroundImage = GameController.renderer.loadImage(R.drawable.levelcomplete);
		wavyBackground = false;
				
		width = 640;
		height = 480;
		
		alpha = 0.75f;
	}
	
	// draw the screen
	public void render(TwoDRenderer renderer) 
	{
		// first draw whatever the parent class wants to draw
		super.render(renderer);
		
		// now draw the stars
		for (int i = 0; i < 5; i++)
		{
			float x = starStartX + (starGap + starSize) * i;
			float y = starY;
			if(i < nStarsToShow)
				MenuScreen.drawImageRelativeCoords(x, y, starSize, starSize, GameController.filledStarImageHandle, renderer);
			else
				MenuScreen.drawImageRelativeCoords(x, y, starSize, starSize, GameController.emptyStarImageHandle, renderer);
		}
	}		

}

class NextLevelButton extends Button
{
	public NextLevelButton()
	{
		width = 160;
		height = 80;
		
		x = 710;
		y = 480;
		
		normalImageHandle = GameController.renderer.loadImage(R.drawable.next);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.next_pressed);		
	}
	
	public void onClicked() 
	{
		GameController.loadNextGameLevel();
	}	
}

// button on the level complte/failed screen
class ExitLevelButton extends Button
{
	public ExitLevelButton()
	{
		width = 160;
		height = 80;
		
		x = 420;
		y = 480;

		normalImageHandle = GameController.renderer.loadImage(R.drawable.quit);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.quit_pressed);		
	}
	
	public void onClicked() 
	{
		GameState.backgroundMusic.pause();
		GameController.showLevelsScreen();
	}	
}

class LevelFailedScreen extends PopupScreen
{		
	RetryButton retryButton;
	ExitLevelButton exitButton;
	public LevelFailedScreen()
	{
		
		retryButton = new RetryButton();
		exitButton = new ExitLevelButton();
		buttons = new ArrayList<Button>(2);
		buttons.add(retryButton);
		buttons.add(exitButton);
		
		backgroundImage = GameController.renderer.loadImage(R.drawable.levelfailed);
		wavyBackground = false;
		
		width = 640;
		height = 480;
		
		alpha = 0.75f;
	}	
}


class RetryButton extends Button
{
	public RetryButton()
	{
		width = 160;
		height = 80;
		
		x = 710;
		y = 480;
		
		normalImageHandle = GameController.renderer.loadImage(R.drawable.retry);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.retry_pressed);		
	}
	
	public void onClicked() 
	{
		GameController.resetCurrentGameLevel();
	}	
}


class GamePausedScreen extends PopupScreen
{		
	ResumeButton resumeButton;
	ExitLevelButton exitButton;
	public GamePausedScreen()
	{
		
		resumeButton = new ResumeButton();
		exitButton = new ExitLevelButton();
		buttons = new ArrayList<Button>(2);
		buttons.add(resumeButton);
		buttons.add(exitButton);
		
		backgroundImage = GameController.renderer.loadImage(R.drawable.pausedscreen);
		wavyBackground = false;
		
		width = 640;
		height = 480;
		
		alpha = 0.75f;
	}	
}

class ResumeButton extends Button
{
	public ResumeButton()
	{
		width = 160;
		height = 80;
		
		x = 710;
		y = 480;
		
		normalImageHandle = GameController.renderer.loadImage(R.drawable.resume);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.resume_pressed);		
	}
	
	public void onClicked() 
	{
		GameController.ResumeGame();
	}	
}


class CalibrateTiltScreen extends MenuScreen
{
	CalibrateOriginButton calibrateOriginButton;
	BackFromCalibrateButton backFromCalibrateButton;
	SliderButton sliderButton;
	
	public CalibrateTiltScreen()
	{
		calibrateOriginButton = new CalibrateOriginButton();
		backFromCalibrateButton = new BackFromCalibrateButton();
		sliderButton = new SliderButton();	
		
		buttons = new ArrayList<Button>(3);
		buttons.add(calibrateOriginButton);
		buttons.add(backFromCalibrateButton);
		buttons.add(sliderButton);

		backgroundImage = GameController.renderer.loadImage(R.drawable.calibrate_tilt_screen);
		wavyBackground = false;
	}	
	
	boolean changedSensitivity = false;
	public void onTouchDownOrMove(int screenX, int screenY)
	{
		super.onTouchDownOrMove(screenX, screenY);
		
		float x = screenX * MenuScreen.WIDTH_UNITS / GameController.screenWidth;
		float y = screenY * MenuScreen.HEIGHT_UNITS / GameController.screenHeight;
		
		if (y > 280 && y < 400 && x > 600 && x < 1280)
		{
			sliderButton.x = Math.max(Math.min(x - sliderButton.width/2, 1120), 700);
			sliderButton.press();
			
			// TODO: remove hardcoded numbers
			Settings.sensitivity = 1 + (sliderButton.x - 700) * 9.0f / (1120 - 700);
			changedSensitivity = true;
		}		
	}
	
	public void onTouchUp(int x, int y)
	{
		sliderButton.release();
		super.onTouchUp(x, y);
		if (changedSensitivity)
		{
			GameController.showMessage("Sensitivity changed to " + Settings.sensitivity, Toast.LENGTH_SHORT);
		}
		changedSensitivity = false;
	}
	
}

class CalibrateOriginButton extends Button
{
	public CalibrateOriginButton()
	{
		width = 240;
		height = 120;
		
		x = 160;
		y = 500;
		
		normalImageHandle = GameController.renderer.loadImage(R.drawable.calibrate);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.calibrate_pressed);		
	}
	
	public void onClicked() 
	{
		GameController.calibrateTilt();
	}	
}

class SliderButton extends Button
{
	public SliderButton()
	{
		// Settings.sensitivity = 1 + (sliderButton.x - 700) * 9.0f / (1120 - 700);
		// x = 700;
		x = 700 + (Settings.sensitivity - 1) * (1120 - 700) / 9.0f;
		y = 300;
		width = 140;
		height = 140;
		
		normalImageHandle = GameController.renderer.loadImage(R.drawable.slider);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.slider_pressed);		
	}
	
	public void onClicked() 
	{
		// do nothing!
	}		
}

class BackFromCalibrateButton extends Button
{
	public BackFromCalibrateButton()
	{
		width = 240;
		height = 120;
		
		x = 840;
		y = 500;
		
		normalImageHandle = GameController.renderer.loadImage(R.drawable.back2);
		pressedImageHandle = GameController.renderer.loadImage(R.drawable.back2_pressed);		
	}
	
	public void onClicked() 
	{
		GameController.showOptionsScreen();
	}	
}