package maach.game;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;


public class GameState implements GameFrameLoop
{
	// size (in pixels) of one block loaded from level file
	public static final int block_size = 16;
	
	// constants to control the water effect
	public static final int water_tex_width = 512;
	public static final int water_tex_height = 512;
	public static final float water_tex_alpha = 0.1f;
	public static final float water_effect_magnitude = 0.02f;		
	public static final float water_effect_time_preiod = 2.0f;		// 2 seconds
	
	
	// applies a low pass filter on frame time to avoid jerky animation
	private static boolean frameTimeSmoothing = true;
	private static double frameTimeSmoothingAlpha = 0.2;
	private double deltaTime = 0;
	
	
	// 8000 pixels = 12 meters (actually 800 pixels = 12 cms, but this works better)	
	public static final float DistanceScale = 8000/12;		
	
	// variables to control motion of player's fish
	public static final float PlayerFishWaterFriction = 0.015f;
	
	// variables to control motion of other fish
	public static final float otherFishWaterFriction = 0.005f;
	
	// A push/impulse/acceleration given to fish after every 2 seconds
	public static final long avgFishMovementDelay = 2000000000l;		// 2 seconds in nanoseconds
	
	// time for which the acceleration is given
	public static final long FishMovementImpulseTime = 300000000l;	// 0.3 seconds in nanoseconds
    	
	// objects in the game
	public ArrayList<Wall> horizontalWalls;
	public ArrayList<Wall> verticalWalls;
	
	public ArrayList<Fish> otherFish;
	public ArrayList<Fish> myFish;
	
	public Goal goal;
	boolean goalReached = false;
	
	// the number of fish eaten
	private int noOfFishEaten;
	
	public ArrayList<Bubble> bubbles;	
	
	// position of mobile screen relative to maze (our background)
	public float screenX, screenY;
	
	// position of player's fish relative to mobile screen
	public float screenPx, screenPy;
	
	public float mazeWidth, mazeHeight;
	public float screenWidth, screenHeight;
	
	// handle of the images/texture that we want to use in our game
	private int horizontalWallTextureHandle;
	private int verticalWallTextureHandle;
	
	private int backgroundWaterTextureHandle;
	
	public static final int nBackTextures = 4;
	private int []backgroundTextureHandles = new int[nBackTextures];
	
	// the current background texture
	private int backgroundTextureHandle;
	
	public static final int nBubbleTextures = 4;
	public int []bubbleTextureHandles = new int[nBubbleTextures];
	
	
	// swimming animation sequences
	AnimationSequence  goluFishAnime;
	AnimationSequence  tigerFishAnime;
	AnimationSequence  angelFishAnime;
	AnimationSequence  clownFishAnime;
	AnimationSequence  saniFishAnime;
	AnimationSequence  sharkFishAnime;
	AnimationSequence  sharkFishEatAnime;
	
	
	
	// eating animation sequences
	AnimationSequence goluFishEatAnime;
	AnimationSequence saniFishEatAnime;
	AnimationSequence angelFishEatAnime;
	AnimationSequence tigerFishEatAnime;
	AnimationSequence clownFishEatAnime;
	
	
	// goal animation sequence
	AnimationSequence goalAnime;
	
	// array containing references to all above swimming fish animations
	public static final int NUM_OTHER_FISH = 4;


	private static final float GOAL_SIZE = 80;

	
	AnimationSequence []otherFishAnime;
	
	
    // array containing references to all above eating fish animations
	AnimationSequence []otherFishEatAnime;
		
	// reference to the renderer object
	TwoDRenderer renderer = null;
	
	// reference to all sound files
	public static MediaPlayer crunchSmall, crunchBig;
	public static MediaPlayer themeSong;
	public static MediaPlayer victory;
	public static MediaPlayer loser;
	public static MediaPlayer backgroundMusic;
	
	// random number generator
	Random randomizer = null;
	
	GameState()
	{
		// nothing to do. Initialization happens in the initialize() method
	}


	// initialization that doesn't depend on level data (called only once)
	public void initialize(Activity act, TwoDRenderer rend, float screenWidth, float screenHeight)
	{
		renderer = rend;

		this.screenHeight = screenHeight;
		this.screenWidth  = screenWidth;
		
	    // by default the player position is in the center of the screen
		screenPx = screenWidth / 2;
		screenPy = screenHeight / 2;
		
		// load the static images to be used inside game
		loadImages();	
		
		// load the sound files into mediaPlayer objects
		loadSounds(act);

		// create animation sequence objects (loading image frames as needed)
		createAnimationSequences();
		
		// create randomizer object
		randomizer = new Random( System.currentTimeMillis() );
		
	}


	private void loadImages()
	{
		// Load the textures using the renderer
		backgroundTextureHandles[0] = renderer.loadImage(R.drawable.background0);
		backgroundTextureHandles[1] = renderer.loadImage(R.drawable.background1);
		backgroundTextureHandles[2] = renderer.loadImage(R.drawable.background2);
		backgroundTextureHandles[3] = renderer.loadImage(R.drawable.background3);				
		
		horizontalWallTextureHandle = renderer.loadImage(R.drawable.h_wall);
		verticalWallTextureHandle = renderer.loadImage(R.drawable.v_wall);
		backgroundWaterTextureHandle = renderer.loadImage(R.drawable.background_water);
		
		//bubbleTextureHandles[0] = renderer.loadImage(R.drawable.bubble);
		bubbleTextureHandles[0] = renderer.loadImage(R.drawable.bubble1);
		bubbleTextureHandles[1] = renderer.loadImage(R.drawable.bubble2);
		bubbleTextureHandles[2] = renderer.loadImage(R.drawable.bubble3);
		bubbleTextureHandles[3] = renderer.loadImage(R.drawable.bubble4);
		
		
	}
	
	private void loadSounds(Activity act)
	{
		crunchSmall = MediaPlayer.create(act, R.raw.munch);
		crunchBig = MediaPlayer.create(act, R.raw.bigcrunch);
		
		themeSong = MediaPlayer.create(act, R.raw.themesong);	
		themeSong.setLooping(true);
		backgroundMusic = MediaPlayer.create(act,R.raw.bubble);
		backgroundMusic.setLooping(true);
		
		victory = MediaPlayer.create(act, R.raw.victory);
		loser = MediaPlayer.create(act, R.raw.loser);
		
		// enable or disable the sounds depending on the setting
		if (Settings.soundsOn)
		{
			GameController.soundsOn();
		}
		else
		{
			GameController.soundsOff();
		}
	}
	
	
	private void createAnimationSequences()
	{
		int fish0, fish1, fish2, fish3, fish4, fish5, fish6, fish7;

		// create the golu fish Animation sequence
		
		fish0 = renderer.loadImage(R.drawable.golu0);
		fish1 = renderer.loadImage(R.drawable.golu1);
		fish2 = renderer.loadImage(R.drawable.golu2);
		fish3 = renderer.loadImage(R.drawable.golu3);

		goluFishAnime = new AnimationSequence(6, 0.8f);
		goluFishAnime.images[0] = fish0;	
		goluFishAnime.images[1] = fish1;
		goluFishAnime.images[2] = fish2;
		goluFishAnime.images[3] = fish3;
		goluFishAnime.images[4] = fish2;	
		goluFishAnime.images[5] = fish1;
		
		// create the tiger fish Animation sequence		
		
		fish0 = renderer.loadImage(R.drawable.tiger0);
		fish1 = renderer.loadImage(R.drawable.tiger1);
		fish2 = renderer.loadImage(R.drawable.tiger2);
		fish3 = renderer.loadImage(R.drawable.tiger3);
		
		tigerFishAnime = new AnimationSequence(6, 0.8f);
		tigerFishAnime.images[0] = fish0;	
		tigerFishAnime.images[1] = fish1;
		tigerFishAnime.images[2] = fish2;
		tigerFishAnime.images[3] = fish3;
		tigerFishAnime.images[4] = fish2;	
		tigerFishAnime.images[5] = fish1;
		
		// create the angel fish Animation sequence		
		fish0 = renderer.loadImage(R.drawable.angel0);
		fish1 = renderer.loadImage(R.drawable.angel1);
		fish2 = renderer.loadImage(R.drawable.angel2);
		fish3 = renderer.loadImage(R.drawable.angel3);
		fish4 = renderer.loadImage(R.drawable.angel4);
		fish5 = renderer.loadImage(R.drawable.angel5);
		fish6 = renderer.loadImage(R.drawable.angel6);
		fish7 = renderer.loadImage(R.drawable.angel7);
		
		angelFishAnime = new AnimationSequence(8, 1f);
		angelFishAnime.images[0] = fish0;
		angelFishAnime.images[1] = fish1;
		angelFishAnime.images[2] = fish2;
		angelFishAnime.images[3] = fish3;
		angelFishAnime.images[4] = fish4;
		angelFishAnime.images[5] = fish5;
		angelFishAnime.images[6] = fish6;
		angelFishAnime.images[7] = fish7;
		
		// create the clown fish Animation sequence		
		fish0 = renderer.loadImage(R.drawable.clown0);
		fish1 = renderer.loadImage(R.drawable.clown1);
		fish2 = renderer.loadImage(R.drawable.clown2);
		fish3 = renderer.loadImage(R.drawable.clown3);
		fish4 = renderer.loadImage(R.drawable.clown4);
		fish5 = renderer.loadImage(R.drawable.clown5);
		fish6 = renderer.loadImage(R.drawable.clown6);
		fish7 = renderer.loadImage(R.drawable.clown7);
		
		clownFishAnime = new AnimationSequence(8, 1f);
		clownFishAnime.images[0] = fish0;
		clownFishAnime.images[1] = fish1;
		clownFishAnime.images[2] = fish2;
		clownFishAnime.images[3] = fish3;
		clownFishAnime.images[4] = fish4;
		clownFishAnime.images[5] = fish5;
		clownFishAnime.images[6] = fish6;
		clownFishAnime.images[7] = fish7;
		
		
		// create the sani fish Animation sequence		
		fish0 = renderer.loadImage(R.drawable.sani0);
		fish1 = renderer.loadImage(R.drawable.sani1);
		fish2 = renderer.loadImage(R.drawable.sani2);
		fish3 = renderer.loadImage(R.drawable.sani3);
		fish4 = renderer.loadImage(R.drawable.sani4);
		fish5 = renderer.loadImage(R.drawable.sani5);
		fish6 = renderer.loadImage(R.drawable.sani6);
		fish7 = renderer.loadImage(R.drawable.sani7);
		
		saniFishAnime = new AnimationSequence(8, 1f);
		saniFishAnime.images[0] = fish0;
		saniFishAnime.images[1] = fish1;
		saniFishAnime.images[2] = fish2;
		saniFishAnime.images[3] = fish3;
		saniFishAnime.images[4] = fish4;
		saniFishAnime.images[5] = fish5;
		saniFishAnime.images[6] = fish6;
		saniFishAnime.images[7] = fish7;
		
		
		// create the shark fish Animation sequence
		
		fish0 = renderer.loadImage(R.drawable.s11);
		fish1 = renderer.loadImage(R.drawable.s22);
		fish2 = renderer.loadImage(R.drawable.s33);
		fish3 = renderer.loadImage(R.drawable.s44);

		sharkFishAnime = new AnimationSequence(6, 0.8f);
		sharkFishAnime.images[0] = fish0;	
		sharkFishAnime.images[1] = fish1;
		sharkFishAnime.images[2] = fish2;
		sharkFishAnime.images[3] = fish3;
		sharkFishAnime.images[4] = fish2;	
		sharkFishAnime.images[5] = fish1;		
		
		
		otherFishAnime = new AnimationSequence[NUM_OTHER_FISH];
		otherFishAnime[0] = tigerFishAnime;
		otherFishAnime[1] = clownFishAnime;
		otherFishAnime[2] = saniFishAnime;
		otherFishAnime[3] = angelFishAnime;		
		
		
		fish0 = renderer.loadImage(R.drawable.sbite1);
		fish1 = renderer.loadImage(R.drawable.sbite2);

		// create the shark fish eat Animation sequence
		
		sharkFishEatAnime = new AnimationSequence(4, 0.6f);
		sharkFishEatAnime.images[0] = fish0;
		sharkFishEatAnime.images[1] = fish1;	
		sharkFishEatAnime.images[2] = fish0;
		sharkFishEatAnime.images[3] = fish1;			
		
		fish0 = renderer.loadImage(R.drawable.golu_eat0);
		fish1 = renderer.loadImage(R.drawable.golu_eat1);
		fish2 = renderer.loadImage(R.drawable.golu_eat2);
		fish3 = renderer.loadImage(R.drawable.golu_eat3);
		fish4 = renderer.loadImage(R.drawable.golu_eat4);

		//create golu fish eating animation sequence
		goluFishEatAnime = new AnimationSequence(8, 0.5f);
		goluFishEatAnime.images[0] = fish1;	
		goluFishEatAnime.images[1] = fish2;
		goluFishEatAnime.images[2] = fish3;
		goluFishEatAnime.images[3] = fish2;
		goluFishEatAnime.images[4] = fish1;
		goluFishEatAnime.images[5] = fish0;
		goluFishEatAnime.images[6] = fish4;		
		goluFishEatAnime.images[7] = fish4;	
		
		
		fish0 = renderer.loadImage(R.drawable.sani_eat0);
		fish1 = renderer.loadImage(R.drawable.sani_eat1);
		fish2 = renderer.loadImage(R.drawable.sani_eat2);
		fish3 = renderer.loadImage(R.drawable.sani_eat3);

		//create sani fish eating animation sequence
		saniFishEatAnime = new AnimationSequence(6, 0.8f);
		saniFishEatAnime.images[0] = fish0;	
		saniFishEatAnime.images[1] = fish1;
		saniFishEatAnime.images[2] = fish2;
		saniFishEatAnime.images[3] = fish3;
		saniFishEatAnime.images[4] = fish2;
		saniFishEatAnime.images[5] = fish1;		

		
		
		fish0 = renderer.loadImage(R.drawable.angel_eat0);
		fish1 = renderer.loadImage(R.drawable.angel_eat1);
		fish2 = renderer.loadImage(R.drawable.angel_eat2);

		
		//create angel fish eating animation sequence
		angelFishEatAnime = new AnimationSequence(5, 0.8f);
		angelFishEatAnime.images[0] = fish1;	
		angelFishEatAnime.images[1] = fish0;
		angelFishEatAnime.images[2] = fish0;
		angelFishEatAnime.images[3] = fish1;
		angelFishEatAnime.images[4] = fish2; 
		
		
		fish0 = renderer.loadImage(R.drawable.tiger_eat0);
		fish1 = renderer.loadImage(R.drawable.tiger_eat1);
		fish2 = renderer.loadImage(R.drawable.tiger_eat2);
		fish3 = renderer.loadImage(R.drawable.tiger_eat3);

		//create tiger fish eating animation sequence
		tigerFishEatAnime = new AnimationSequence(6, 0.8f);
		tigerFishEatAnime.images[0] = fish0;	
		tigerFishEatAnime.images[1] = fish1;
		tigerFishEatAnime.images[2] = fish2;
		tigerFishEatAnime.images[3] = fish1;
		tigerFishEatAnime.images[4] = fish0;
		tigerFishEatAnime.images[5] = fish3;		

		fish0 = renderer.loadImage(R.drawable.clown_eat0);
		fish1 = renderer.loadImage(R.drawable.clown_eat1);
		fish2 = renderer.loadImage(R.drawable.clown_eat2);
		fish3 = renderer.loadImage(R.drawable.clown_eat3);

		//create tiger fish eating animation sequence
		clownFishEatAnime = new AnimationSequence(6, 0.8f);
		clownFishEatAnime.images[0] = fish0;	
		clownFishEatAnime.images[1] = fish1;
		clownFishEatAnime.images[2] = fish2;
		clownFishEatAnime.images[3] = fish1;
		clownFishEatAnime.images[4] = fish0;
		clownFishEatAnime.images[5] = fish3;		
		
		
		otherFishEatAnime = new AnimationSequence[NUM_OTHER_FISH];
		otherFishEatAnime[0] = tigerFishEatAnime;
		otherFishEatAnime[1] = clownFishEatAnime;
		otherFishEatAnime[2] = saniFishEatAnime;
		otherFishEatAnime[3] = angelFishEatAnime;
		
	
		
		
		fish0 = renderer.loadImage(R.drawable.g1);
		fish1 = renderer.loadImage(R.drawable.g2);
		fish2 = renderer.loadImage(R.drawable.g3);
		fish3 = renderer.loadImage(R.drawable.g4);
		fish4 = renderer.loadImage(R.drawable.g5);
		fish5 = renderer.loadImage(R.drawable.g6);
		fish6 = renderer.loadImage(R.drawable.g7);
		fish7 = renderer.loadImage(R.drawable.g8);
		

		
		//create goal animation sequence
		goalAnime = new AnimationSequence(8, 0.8f);
		goalAnime.images[0] = fish0;	
		goalAnime.images[1] = fish1;
		goalAnime.images[2] = fish2;
		goalAnime.images[3] = fish3;
		goalAnime.images[4] = fish4;
		goalAnime.images[5] = fish5;
		goalAnime.images[6] = fish6;
		goalAnime.images[7] = fish7;
					
	
	}
	
	
	// load a new level into this GameState object
	// called everytime a new level is loaded
	public void loadLevel(LevelLoader loader, int level, int backTexIndex)
	{
		// re-create objects that change with level
		horizontalWalls = new ArrayList<Wall>();
		verticalWalls 	= new ArrayList<Wall>();
		otherFish 		= new ArrayList<Fish>();
		myFish 			= new ArrayList<Fish>();
		goal            = new Goal();
		
		// use levelLoader class to load level data from file
	    loader.loadLevel(this, level);
	    
	    // set the current background image
	    assert(backTexIndex > 0 && backTexIndex < nBackTextures);
	    backgroundTextureHandle = backgroundTextureHandles[backTexIndex]; 
	    
	    // rest of initialization based on the loaded level
	    
		// set size of the goal
		goal.size = GOAL_SIZE;
		
		// make the fish object point to the animation sequences and assign other properties
		int n = myFish.size();
		for (int i = 0; i < n; i++)
		{
			myFish.get(i).fishAnime = goluFishAnime;
			myFish.get(i).fishEatAnime  = goluFishEatAnime;
			myFish.get(i).size = Fish.DEFAULT_SIZE;
			myFish.get(i).waterFriction = PlayerFishWaterFriction;
		}

		n = otherFish.size();
		for (int i = 0; i < n; i++)
		{
			Fish fish = otherFish.get(i);

			
			if(fish.fishType >= '0' && fish.fishType <= '9')
			{
				fish.fishAnime = otherFishAnime[i % NUM_OTHER_FISH];
				fish.fishEatAnime = otherFishEatAnime[i % NUM_OTHER_FISH];				
				fish.size = Fish.MIN_SIZE + Fish.SIZE_MULTIPLIER * (fish.fishType - '0');
			}
			
			else if(fish.fishType == 's')
			{
				fish.fishAnime = sharkFishAnime;
				fish.fishEatAnime = sharkFishEatAnime;				
				fish.size = Fish.SHARK_SIZE;
			}
			else
			{
				assert(false);
			}
			fish.waterFriction = otherFishWaterFriction;
		}
		
		// create the bubble arraylist
		// no of bubbles = area of maze (in pixels) / 100000
		int nBubbles = (int) (mazeWidth * mazeHeight / 100000);
		bubbles = new ArrayList<Bubble>(nBubbles);
		for(int i=0; i< nBubbles; i++)
		{
			bubbles.add(new Bubble());
		}
		
		paused = false;
		noOfFishEaten = 0;
		
		lastTime = 0;
		deltaTime = 0;
	}	
	
	
	// methods implementing GameFrameLoop
	
	// used for various timing calculations
	private long lastTime = 0;
	private long currentTime = 0;
	
	// for controlling motion of other fish
	private long nextFishUpdateTime = 0;
	
	
	// for exiting from the game after the game is over
	private long gameOverExitTime;
	private static final long gameOverDelay = 1000000000l;	// game over delay (time in nanoseconds to wait before exiting from the game)
	private boolean levelComplete;
	
	
	public void updatePositions() 
	{
		// update positions
		if(lastTime != 0)
		{
			// time elapsed between this frame and the last
			double timeDiff = (double) (currentTime - lastTime) / 1000000000.0;
			
			if(frameTimeSmoothing)
			{
				timeDiff = timeDiff * frameTimeSmoothingAlpha + deltaTime * (1.0 - frameTimeSmoothingAlpha);
				deltaTime = timeDiff;
			}
			
			// update positions of various objects in the game (like fish, bubbles, etc)
			
		
			// 1. Player's fish
			Fish playerFish = myFish.get(0);
			
			// update position (also handles collision with walls)
			playerFish.updatePosition((float) timeDiff, this, currentTime, true);
					
			
			// Make the viewport move with the player's fish (trying to keep player's fish in the center of the screen)
			screenX = playerFish.x - screenPx;
			screenY = playerFish.y - screenPy;
			
			// don't allow the viewport to go outside the maze
			if(screenX < 0)
			{
				screenX = 0;
			}
			else
			{
				float limitRight = Math.abs(mazeWidth - screenWidth);
				if(screenX > limitRight) screenX = limitRight;
			}
			if(screenY < 0)
			{
				screenY = 0;
			}
			else
			{
				float limitBottom = Math.abs(mazeHeight - screenHeight);
				if(screenY > limitBottom) screenY = limitBottom;
			}
			
			
			
			// 2. Other fish
			
			int nFish = otherFish.size();
			
			// select a fish randomly and give it a Push
			if (currentTime >= nextFishUpdateTime)
			{
								
				int fishIndex = randomizer.nextInt(nFish);
				otherFish.get(fishIndex).givePush(this, currentTime);
			
				// figure out when we need to give a push to some fish again
				long fishMovementInterval = avgFishMovementDelay / nFish;
				nextFishUpdateTime = currentTime + fishMovementInterval;
			}
			
			// move the other fishes too
			// (also handles collision with walls)
			for (int i = 0; i < nFish; i++)
			{
				otherFish.get(i).updatePosition((float) timeDiff, this, currentTime, false);
			}		
			
			// 3. bubbles
			int nBubble = bubbles.size();
			for(int i=0; i < nBubble; i++)
			{
				bubbles.get(i).updatePosition((float) timeDiff, currentTime, randomizer, this);
			}
			
			// 4. Goal (checks if the player's fish has reached the goal)
			goalReached = goal.checkIfPlayerArrived(this);
			if(goalReached)
			{
				levelComplete = true;
				if(playerFish.alive)
				{
					gameOverExitTime = currentTime + gameOverDelay / 2;
					playerFish.alive = false;
				}
			}
			
			
			if (!playerFish.alive && currentTime > gameOverExitTime)
			{
				
				// HACK!! Avoid calling gameFinished again and again 
				// As GameState.render would get called from levelComplete/levelFailed screens, the control will come here
				// and make us unnecessarily call gameFinished again and again (making irritating repeating noises :-/)
				// hack not working for some reason :(
				gameOverExitTime =	currentTime + 3600l * 1000000000l;		// delay for 1 hour :)
				
				GameController.gameFinished(levelComplete, noOfFishEaten);
			}
		}
	}

	public boolean paused = false; 
	
	// for fps counting
	private static final int framesAvg = 20;
	private int  frameNumber = 0;
	private long lastSavedTime = 0;

	// for logging
	private static final String TAG = "maach";	
	 
	// clear with light blue color and render the fish
	public void render(TwoDRenderer renderer) 
	{
		
		// first of all get the current time
		currentTime = System.nanoTime();
		
		
		// For FPS counting : start
		frameNumber++;
		if(frameNumber % framesAvg == 0)
		{
			Log.v(TAG, "fps: " + (framesAvg * 1000000000.0 / (currentTime - lastSavedTime)));
			lastSavedTime = currentTime;
		}
		// For FPS counting : end		

		
		if(!paused)
		{
			updatePositions();
		}
		
		lastTime = currentTime;

		
		// clear the screen with a suitable color
		renderer.clearScreen(90, 190, 240, 255);
		
		// draw various objects
		
		// 1. Background image
		// > 50% perf drop just to draw the wavy background :-/
		if (Settings.waterOn)
		{
			drawWavyBackgroundRelativeToMaze(backgroundTextureHandle, mazeWidth/2, mazeHeight/2, mazeWidth, mazeHeight, 0);
		}
		else
		{
			drawObjectRelativeToMaze(backgroundTextureHandle, mazeWidth/2, mazeHeight/2, mazeWidth, mazeHeight, 0);
		}

		
		// 2. bubbles
		int n = bubbles.size();
		for (int i = 0; i < n; i++)
		{
			bubbles.get(i).draw(this);
		}
		
		// 3. maze walls
		drawMazeWalls();
		
		// 4. other fish
		n = otherFish.size();
		for (int i = 0; i < n; i++)
		{
			otherFish.get(i).draw(this, currentTime);
		}
		
		// 5. player's fish
		myFish.get(0).draw(this, currentTime);
		
		// 6. the 'goal' object
		goal.draw(this, currentTime, goalAnime);
						
	}
	
	public void drawObjectRelativeToMaze(int imageHandle, float maze_x, float maze_y, float width, float height, float angle)
	{
		// find the position of the object relative to the screen
		float positionX = maze_x - screenX;	
		float positionY = maze_y - screenY;
		
		renderer.drawImageScaled(imageHandle, positionX, positionY, width, height, angle);		
	}

	private void drawWavyBackgroundRelativeToMaze(int imageHandle, float maze_x, float maze_y, float width, float height, float angle)
	{
		// find the position of the object relative to the screen
		float positionX = maze_x - screenX;	
		float positionY = maze_y - screenY;
		
		//renderer.drawWavyImageScaled(imageHandle, positionX, positionY, width, height, angle, currentTime);
		renderer.drawBlendedWavyImages(imageHandle, backgroundWaterTextureHandle, positionX, positionY, width, height, angle, water_tex_alpha, currentTime, 
									water_effect_time_preiod, water_effect_magnitude, width / water_tex_width, height / water_tex_height);
	}
	
	
	private void drawMazeWalls()
	{
		// Draw the horizontal walls
		int n = horizontalWalls.size();
		for (int i = 0; i < n; i++)
		{
			horizontalWalls.get(i).draw(this, horizontalWallTextureHandle);
		}
		
		// Draw the vertical walls
		n = verticalWalls.size();
		for (int i = 0; i < n; i++)
		{
			verticalWalls.get(i).draw(this, verticalWallTextureHandle);			
		}
		
	}
	
	public static boolean checkCollisionRectangles(float x1, float y1, float w1, float h1, 
										   		   float x2, float y2, float w2, float h2)
	{	    		
		if ((x1 + w1 >= x2) &&
		    (x1 <= x2 + w2) &&
		    (y1 + h1 >= y2) &&
		    (y1 <= y2 + h2)
		   )
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void respondToCollisionBetweenFishes(Fish collidedFish, GameState game)
	{
		Fish myFish = game.myFish.get(0);
		
		if(myFish.size  >= collidedFish.size && collidedFish.fishType != 's')
		{
			// kill the other fish (whom I have eaten)
			collidedFish.alive = false;
			
			// increment the no. of fish eaten
			noOfFishEaten = noOfFishEaten + 1; 
			
			// grow player's fish
			// note that the growth is actually deferred to avoid collision on growth
			if (myFish.size < Fish.MAX_SIZE)
			{
				myFish.pendingGrowth = true;
			}
			
			// sound effect of munching
			crunchSmall.start();	
			
			// replace player fish swimming animation with eating animation
			myFish.eating = true;
			myFish.eatTime = currentTime + myFish.fishEatAnime.duration * 1000000000; 
			
		}
		else
		{
			// game over - a bigger fish or shark ate player's fish :(
			
			levelComplete = false;
			if(myFish.alive)
			{
				gameOverExitTime = currentTime + gameOverDelay;
			}
			
			collidedFish.eating  = true;
			collidedFish.eatTime = currentTime + collidedFish.fishEatAnime.duration * 1000000000;
			myFish.alive = false;	
			crunchBig.start();
		}				
	}

	
};
