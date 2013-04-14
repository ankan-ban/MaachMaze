package maach.game;

import java.util.Random;

public class Wall {
	// the starting position of the wall (of it's top left corner)
	public float x, y;
	
	// width and height of the wall
	public float width, height;	
	
	// draw the wall
	public void draw(GameState game, int textureHandle)
	{	
		float x1, y1;
		
		float offsetCentreX = 0, offsetCentreY = 0;
		
		offsetCentreX = width / 2;
		offsetCentreY = height / 2;
		
		x1 = x + offsetCentreX;
		y1 = y + offsetCentreY;
		
		game.drawObjectRelativeToMaze(textureHandle, x1, y1, width, height, 0.0f);		
	}
};

// TODO: move this to a separate file if needed / or if this becomes too big
// a bubble keeps moving towards up direction (TODO: maybe move it in a slightly random fashion)
// A bubble will burst (disappear) after some time. 
// It never dies after disappearing but will re-appear again at some random location in the maze
// TODO: maybe it's wasteful to compute/try rendering bubbles in the space that the player can't see, optimize if this becomes a bottleneck

class Bubble
{
	private static final float MIN_BUBBLE_SIZE =  5;		// radius in pixels	 
	private static final float MAX_BUBBLE_SIZE = 40;
	
	private static final int MAX_BUBBLE_LIFE = 15000;	// 15 seconds in milliseconds
	
	private static final float MIN_BUBBLE_SPEED = 1;	// pixels per seconds
	private static final float MAX_BUBBLE_SPEED = 100;
	
	private float x, y;
	private float size; // radius
	private float speed;
	private long nextTimeofBrust = 0;
	private int textureHandle;
	
	public void updatePosition(float timeDiff, long currentTime, Random randomizer, GameState game)
	{
		y = y - speed * timeDiff;	// bubbles always move up with constant velocity
		
		if(currentTime > nextTimeofBrust || y < 0.0)
		{
			// randomly choose speed
			speed = randomizer.nextFloat() * (MAX_BUBBLE_SPEED - MIN_BUBBLE_SPEED) + MIN_BUBBLE_SPEED;
			
			// randomly choose size
			size = randomizer.nextFloat() * (MAX_BUBBLE_SIZE - MIN_BUBBLE_SIZE) + MIN_BUBBLE_SIZE;
			
			// randomly choose position
			x = randomizer.nextInt((int) game.mazeWidth);
			y = randomizer.nextInt((int) game.mazeHeight/2) + game.mazeHeight/2;	// bubbles always start at bottom half of the maze 
			
			int life = randomizer.nextInt(MAX_BUBBLE_LIFE);
			nextTimeofBrust = currentTime + life * 1000000l;		// convert milliseconds to nanoseconds
			
			// randomly assign a texture handle
			int textureIndex = randomizer.nextInt(game.nBubbleTextures);
			textureHandle = game.bubbleTextureHandles[textureIndex];
		}
	}
	
	void draw(GameState game)
	{
		// TODO maybe rotate the bubble for added effect ?
		game.drawObjectRelativeToMaze(textureHandle, x, y, size, size, 0.0f);				
	}
	
};