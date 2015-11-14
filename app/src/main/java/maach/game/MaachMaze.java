package maach.game;


import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MaachMaze extends Activity 
{
	
	public static int screenWidth, screenHeight, screenRotation;
	
	// references to other class objects
	private TwoDRenderer renderer = null;	
	private GameController gameController = null;
	private SensorManager sensorManager = null;
	

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
	
		// don't want any title for the app
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// don't want automatic orientation change
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		// don't want the status bar on top
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// don't dim the screen
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		
		// get screen dimensions
		Display display = getWindowManager().getDefaultDisplay();
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();
		screenRotation = display.getRotation();

		// call super class (always needed for android apps)
		super.onCreate(savedInstanceState);
		
		// read all the settings from settings file
		readSettingsFromFile();
		
		// create the renderer
		View touchEventView = null;
		if (Settings.useOpengl)
		{
			OpenGLRenderer oRenderer = new OpenGLRenderer(this);
			setContentView(oRenderer);
			renderer = oRenderer;
			touchEventView = oRenderer;
		}
		else	// use canvas renderer
		{
			CanvasRenderer cRenderer = new CanvasRenderer(this);
			setContentView(cRenderer);
			renderer = cRenderer;
			touchEventView = cRenderer;			
		}
				       
		// Get a reference to a SensorManager
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		// create the game controller object
		gameController = new GameController(this, renderer, screenWidth, screenHeight, screenRotation);
		
        // Touch event handling done by gameController
		touchEventView.setOnTouchListener(gameController);
	}
	
	@Override
	public void onBackPressed() {
		// do something on back.
		GameController.onBackPressed();
		return;
	}	

	protected void onResume() {
		super.onResume();
		
		renderer.onResume();
		
		Log.v("maach", "maach: resumed");		
		
		// Register the GameController class as a listener for the accelerometer sensor
		sensorManager.registerListener(gameController,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
	}
	
	protected void onPause(){
		super.onPause();
		renderer.onPause();
		
		Log.v("maach", "maach: paused");
		
		// Unregister the listener
		sensorManager.unregisterListener(gameController);
		super.onStop();
				
	}

	protected void onStop() {
		
		GameState.crunchBig.stop();
		GameState.crunchSmall.stop();
		GameState.themeSong.stop();
		GameState.victory.stop();
		GameState.loser.stop();
		GameState.backgroundMusic.stop();
		
		GameState.crunchBig.release();
		GameState.crunchSmall.release();
		GameState.themeSong.release();
		GameState.victory.release();
		GameState.loser.release();
		GameState.backgroundMusic.release();
		
		// Unregister the listener
		sensorManager.unregisterListener(gameController);
		super.onStop();
		
		
		
		// Save settings to file
		writeSettingsToFile();
	}
	
	
	private boolean detectOpenGLES20() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return (info.reqGlEsVersion >= 0x20000);
    }
	
	public static synchronized int readTotalRam() 
	{ 
		int tm=1000; 
		try { 
			RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r"); 
			String load = reader.readLine(); 
			String[] totrm = load.split(" kB"); 
			String[] trm = totrm[0].split(" "); 
			tm=Integer.parseInt(trm[trm.length-1]); 
			tm=Math.round(tm/1024); 
		} catch (IOException ex) 
		{ ex.printStackTrace(); 
		} 
		return tm; 
	}
	
	public static final String SETTINGS_NAME = "MaachGameSettings";	
	
	private void readSettingsFromFile()
	{
		SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
		
		int memorySize = readTotalRam();
		Log.v("MaachMaze", "RAM size: " + memorySize);
		boolean defaultOpenGLRenderer = true;
		
		if (memorySize < 256)
		{
			defaultOpenGLRenderer = false;
		}
		
		
		Settings.useOpengl = settings.getBoolean("useOpengl", defaultOpenGLRenderer);	
		Settings.soundsOn = settings.getBoolean("soundsOn", true);
		Settings.useTouchInput = settings.getBoolean("useTouch", false);
		Settings.waterOn = settings.getBoolean("waterOn", true);
		
		Settings.sensitivity =  settings.getFloat("sensitivity", 2.0f);
		Settings.neutral_Ax =  settings.getFloat("neutral_Ax", 0.0f);
		Settings.neutral_Ay =  settings.getFloat("neutral_Ay", 0.0f);
		
		// force canvas mode if openGL ES 2.0 isn't supported on the phone
		Settings.hasOpenGLSupport = detectOpenGLES20(); 
		if (!Settings.hasOpenGLSupport) 
		{
			Settings.useOpengl = false;
		}
				
	}
	
	private void writeSettingsToFile()
	{
		  SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
		  SharedPreferences.Editor editor = settings.edit();
		  editor.putBoolean("useOpengl", Settings.useOpengl);
		  editor.putBoolean("soundsOn", Settings.soundsOn);
		  editor.putBoolean("useTouch", Settings.useTouchInput);
		  editor.putBoolean("waterOn", Settings.waterOn);
		  editor.putFloat("sensitivity", Settings.sensitivity);
		  editor.putFloat("neutral_Ax", Settings.neutral_Ax);
		  editor.putFloat("neutral_Ay", Settings.neutral_Ay);		  
		
		  // write level information
		  editor.putInt("maxLevel", GameController.maxLevel);
		  editor.putInt("starsLevel1", GameController.levelData[0].starsWon);
		  editor.putInt("starsLevel2", GameController.levelData[1].starsWon);
		  editor.putInt("starsLevel3", GameController.levelData[2].starsWon);
		  editor.putInt("starsLevel4", GameController.levelData[3].starsWon);
		  editor.putInt("starsLevel5", GameController.levelData[4].starsWon);
		  editor.putInt("starsLevel6", GameController.levelData[5].starsWon);
		  editor.putInt("starsLevel7", GameController.levelData[6].starsWon);
		  editor.putInt("starsLevel8", GameController.levelData[7].starsWon);
		  editor.putInt("starsLevel9", GameController.levelData[8].starsWon);
		  editor.putInt("starsLevel10", GameController.levelData[9].starsWon);

		  // Commit the edits
		  editor.commit();		
	}
	
	public void readLevelDataFromFile()
	{
		// read level information
		SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
		GameController.maxLevel = settings.getInt("maxLevel", 0);
		
		GameController.levelData[0].starsWon = settings.getInt("starsLevel1", 0);
		GameController.levelData[1].starsWon = settings.getInt("starsLevel2", 0);
		GameController.levelData[2].starsWon = settings.getInt("starsLevel3", 0);
		GameController.levelData[3].starsWon = settings.getInt("starsLevel4", 0);
		GameController.levelData[4].starsWon = settings.getInt("starsLevel5", 0);
		GameController.levelData[5].starsWon = settings.getInt("starsLevel6", 0);
		GameController.levelData[6].starsWon = settings.getInt("starsLevel7", 0);
		GameController.levelData[7].starsWon = settings.getInt("starsLevel8", 0);
		GameController.levelData[8].starsWon = settings.getInt("starsLevel9", 0);
		GameController.levelData[9].starsWon = settings.getInt("starsLevel10", 0);
	}
	
}