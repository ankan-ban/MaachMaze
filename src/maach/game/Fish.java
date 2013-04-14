package maach.game;

public class Fish
{
	public static final int MIN_SIZE = 24;			// size of smallest fish
	public static final int SIZE_MULTIPLIER = 5;	// size increment for every fish type increment
	// e.g: smallest fish of type '0' is of size 24 pixels (radius)
	// largest fish of size 9 is of size: 24 + 9*5 = 69
	public static final int DEFAULT_SIZE = 48;
	public static final int SHARK_SIZE = 58;
	public static final int MAX_SIZE = 69;
	
	public static final float constantFriction = 2.0f;
			
	// the proximities are in square of distance
	public static final float FISH_EAT_PROXIMITY = 30 * 30;
	public static final float FISH_INTERACTION_PROXIMITY = 250 * 250;
	
	// approximate position of the center of mouth of the fish
	public static final float FISH_SIZE_TO_MOUTH_RATIO = 0.8f;
	
	// the acceleration with which the other fish will avoid you or come to eat you
	public static final float FISH_INTERACTION_ACCELERATION = 1.2f;
	
	
	// position (of the center of the fish)
	float x, y;
	
	// speed
	float vx, vy;
	
	// acceleration
	float ax, ay;
	
	// angle, 0 degree means facing towards the right
	// angle is atan(vy/vx), but we add a low pass filter to make the transition smooth on collisions
	float angle;
	
	// type/size of the fish
	// '0' - smallest fish
	// '2' - small fish
	// '3' - average size fish
	// '5' - big fish
	// '9' - biggest fish
	// 'p' - player fish
	// 's' - shark
	char  fishType;
	
	boolean alive = true;
	boolean eating = false;
	boolean pendingGrowth = false;
	
	// Animation Sequence reference
	AnimationSequence fishAnime, fishEatAnime;
	
	//the fish is always a square or circle, so the size is half the size of side or radius
	float size;

	float waterFriction;	// water friction for this fish	
	float nextImpulseEndTime = 0;
	float eatTime = 0;
	
	
	//public static final float coeffRestitution = 0.0f;
	//public static final float constantFric = 0.1f*GameState.DistanceScale;

	// constants used for fish-wall collision resolution
	private static final int HORIZONTAL_COLLISION 		 = 1;
	private static final int VERTICAL_COLLISION 	 	 = 2;
	private static final int HORIZONTAL_COLLISION_CORNER = 4;
	private static final int VERTICAL_COLLISION_CORNER   = 8;
	private static final int HORIZONTAL_COLLISION_MASK 	 = HORIZONTAL_COLLISION | HORIZONTAL_COLLISION_CORNER;  
	private static final int VERTICAL_COLLISION_MASK 	 = VERTICAL_COLLISION 	| VERTICAL_COLLISION_CORNER;  
	private static final int NORMAL_COLLISION_MASK       = HORIZONTAL_COLLISION | VERTICAL_COLLISION;
	// private static final int CORNER_COLLISION_MASK       = HORIZONTAL_COLLISION_CORNER | VERTICAL_COLLISION_CORNER;	// not used yet
	
	
	public void updatePosition(float timeDiff, GameState game, long currentTime, boolean playerFish)
	{
		// if the fish is dead, no need to do anything
		if (!alive)
		{
			return;
		}
		
		
		// v = u + at
		vx = vx + ax * timeDiff * GameState.DistanceScale;
		vy = vy + ay * timeDiff * GameState.DistanceScale;
		
		
		// account for friction - proportional to square of the velocity in fluid
		// constant friction can be useful if we want the fish to ultimately stop

		float fricX;
		float fricY;
		if (playerFish)
		{
			fricX = (constantFriction + waterFriction * vx * vx) * timeDiff;
			fricY = (constantFriction + waterFriction * vy * vy) * timeDiff;
		}
		else
		{
			// no constant friction for other fish
			fricX = (waterFriction * vx * vx) * timeDiff;
			fricY = (waterFriction * vy * vy) * timeDiff;			
		}
		

		if (fricX > Math.abs(vx)) {
			vx = 0;
		}
		else {
			vx = (Math.abs(vx) - fricX)*Math.signum(vx);
		}

		if (fricY > Math.abs(vy)) {
			vy = 0;
		}
		else {
			vy = (Math.abs(vy) - fricY)*Math.signum(vy);
		}
		
		
		float previousX = x;
		float previousY = y;
		
		
		float distanceX = vx * timeDiff;
		x = x + distanceX;

		float distanceY = vy * timeDiff;
		y = y + distanceY;

		// update the angle (direction) the fish is looking
		updateFishAngle();

		// Collision with walls (this can undo the change in x and y, and might set vx, vy to zero if collided)
		handleFishWallCollision(game, previousX, previousY, distanceX, distanceY);
		
		if (pendingGrowth)
		{
			// try growing the fish
			size += Fish.SIZE_MULTIPLIER;
			
			// check if the fish will collide with any walls after the growth
			int nHoriCollided = checkCollisionWithHorizontalWalls(game);
			int nVertiCollided = checkCollisionWithVerticalWalls(game);
			if (nHoriCollided == 0 && nVertiCollided == 0)
			{
				// growth successful
				pendingGrowth = false;				
			}
			else
			{
				// undo the growth as it will get inside wall
				size -= Fish.SIZE_MULTIPLIER;
			}
		}
		
		
		// collision / interaction with other fish
		// note that we check collision of player's fish with every other fish
		// other fishes don't do anything when they collide with each other
		if (!playerFish)
		{
			// end the push/acceleration
			if(currentTime > nextImpulseEndTime)
			{
				ax = 0;
				ay = 0;
			}
			
			handleInteractionWithPlayerFish(game);

		}
		
		if (currentTime  > eatTime)
		{
			eating = false;			
		}
	}
	
	
	private void updateFishAngle()
	{
		// figure out the angle based on the ratio of velocities in x and y directions
		float curAngle = (float) ((float) Math.atan2(vy, vx) * 180.0f / Math.PI);
	
		// update angle after applying a low pass filter
		final float alpha = 0.8f;
		// HACK: we want the rotation to happen in a cylindrical way. 
		// the atan2 function is discontinuous at +/-pi, i.e, the return
		// value is from 0 to pi and from 0 to -pi. If the two angles
		// we are interpolating between lie at different ends of this discontinuity,
		// we would get a long roundabout rotation E.g for angles 175 degree and -175 degrees
		// we want a counter-clockwise rotation of 10 degrees, but because of this discontinuity
		// if we interpolate directly, we will get a roundabout rotation of 350 degrees in clockwise direction
		
		// to avoid this, figure out if the difference in the angles is more than 180 degrees
		// and if so, add 360 to the negative angle, do the interpolation and then make it again between -180 to 180
		// TODO: maybe there is a neater/more efficient solution for this :-/
		if (angle < 0 )
		{
			if(curAngle > 0)
			{
				if (curAngle - angle > 180.0f)
					angle += 360.0f;
				
			}
		}
		else if (curAngle < 0)
		{
			if(angle > 0)
			{
				if (angle - curAngle > 180.0f)
					curAngle += 360.0f;
			}
		}
		
		angle = alpha * angle + (1.0f - alpha) * curAngle;
		
		if (angle > 180.0f)
		{
			angle -= 360.0f;
		}		
	}
	
	
	
	// temp references used during wall-fish collision detection
	static Wall wallVert;
	static Wall wallHori;	
		
	private void handleFishWallCollision(GameState game, float previousX, float previousY, float distanceX, float distanceY)
	{
		wallVert = null;
		wallHori = null;
		
		// find out the no. of horizontal and vertical walls the fish collided with
		int nHoriCollided = checkCollisionWithHorizontalWalls(game);
		int nVertiCollided = checkCollisionWithVerticalWalls(game);
		
		// if the fish collided at all
		if (nHoriCollided > 0 || nVertiCollided > 0)
		{
			// check if the fish collided with more than 1 wall of each kind (horizontal and vertical)
			if (nHoriCollided > 1)
			{
				x = previousX; vx = 0;
				
				// check if still colliding with vertical wall
				if (nVertiCollided > 0)
				{
					boolean stillColliding = checkCollisionWithSingleWall(wallVert);
					if (!stillColliding)
						nVertiCollided = 0;
				}
				
			}
			if (nVertiCollided > 1)
			{
				y = previousY; vy = 0;

				// check if still colliding with horizontal wall
				if (nHoriCollided > 0)
				{
					boolean stillColliding = checkCollisionWithSingleWall(wallHori);
					if (!stillColliding)
						nHoriCollided = 0;
				}				
			}
			
			// if collided with both kinds of walls
			if (nHoriCollided > 0 && nVertiCollided > 0)
			{
				int hColliType = checkCollisionType(wallHori, previousX, previousY, distanceX, distanceY);
				int vColliType = checkCollisionType(wallVert, previousX, previousY, distanceX, distanceY);
				int collisionMask = hColliType | vColliType;
				if ((collisionMask & NORMAL_COLLISION_MASK) != 0)
				{	// normal collision takes precedence over corner collision
					if ((collisionMask & HORIZONTAL_COLLISION) != 0)
					{
						x = previousX; vx = 0;
					}
					if((collisionMask & VERTICAL_COLLISION) != 0)
					{
						y = previousY; vy = 0;						
					}
				}
				else
				{
					if ((collisionMask & HORIZONTAL_COLLISION_CORNER) != 0)
					{
						x = previousX; vx = 0;
					}
					if((collisionMask & VERTICAL_COLLISION_CORNER) != 0)
					{
						y = previousY; vy = 0;						
					}
				}
			}
			// collided with either only horizontal wall (s) or only vertical wall(s)
			else 
			{
				// TODO: missing case - e.g, if more than one horizontal walls collided and the one in wallHori isn't vertical collision, but some other is :(
				
				int colliType = 0;
				if (nHoriCollided > 0)
					colliType = checkCollisionType(wallHori, previousX, previousY, distanceX, distanceY);
				else if (nVertiCollided > 0)
					colliType = checkCollisionType(wallVert, previousX, previousY, distanceX, distanceY);
				
				if ((colliType & HORIZONTAL_COLLISION_MASK) != 0)
				{
					x = previousX; vx = 0;		
				}
				else if ((colliType & VERTICAL_COLLISION_MASK) != 0)
				{
					y = previousY; vy = 0;					
				}
			}
		} // if collided at all		
	}
	
	private int checkCollisionType(Wall wall, float previousX, float previousY, float distanceX, float distanceY)
	{
		if(checkCollisionWithSingleWall(wall, previousX, y))
		{
			return VERTICAL_COLLISION;
		}
		else if(checkCollisionWithSingleWall(wall, x, previousY))
		{
			return HORIZONTAL_COLLISION;
		}
		else if(distanceX > distanceY)
		{
			return VERTICAL_COLLISION_CORNER;
		}
		else	// if (distance y >= distanceX)
		{
			return HORIZONTAL_COLLISION_CORNER;
		}
	}
	
	
	
	private int checkCollisionWithHorizontalWalls(GameState game)
	{
		int nCollisions = 0;
		
    	// top left corner of the fish
		float topLeftX = x - size;
		float topLeftY = y - size; 
		
		int nWalls = game.horizontalWalls.size();
		for(int i = 0; i < nWalls; i++ )
		{
			Wall wall = game.horizontalWalls.get(i);
			boolean hit = GameState.checkCollisionRectangles(wall.x, wall.y, wall.width, wall.height, topLeftX, topLeftY, 2 * size, 2 * size);				     
			if (hit)
			{
				nCollisions++;
				// no need to track more than 1 collision
				if (nCollisions > 1)
					return nCollisions;

				wallHori = wall;
			}
		}
		
		return nCollisions;
	}
	
	private int checkCollisionWithVerticalWalls(GameState game)
	{
		int nCollisions = 0;
		
		// top left corner of the fish
		float topLeftX = x - size;
		float topLeftY = y - size; 
		
		int nWalls = game.verticalWalls.size();
		for(int i = 0; i < nWalls; i++ )
		{
			Wall wall = game.verticalWalls.get(i); 	
			boolean hit = GameState.checkCollisionRectangles(wall.x, wall.y, wall.width, wall.height, topLeftX, topLeftY, 2 * size, 2 * size);							     
			if (hit) 
			{
				nCollisions++;
				// no need to track more than 1 collision
				if (nCollisions > 1)
					return nCollisions;
				
				wallVert = wall;
			}
		}
				
		return nCollisions;
	}
		
	private boolean checkCollisionWithSingleWall(Wall wall)
	{
		// top left corner of the fish
		float topLeftX = x - size;
		float topLeftY = y - size; 
		boolean hit = GameState.checkCollisionRectangles(wall.x, wall.y, wall.width, wall.height, topLeftX, topLeftY, 2 * size, 2 * size);							     
		return hit;
	}

	// this version takes x, y as paramaters
	private boolean checkCollisionWithSingleWall(Wall wall, float x, float y)
	{
		// top left corner of the fish
		float topLeftX = x - size;
		float topLeftY = y - size; 
		boolean hit = GameState.checkCollisionRectangles(wall.x, wall.y, wall.width, wall.height, topLeftX, topLeftY, 2 * size, 2 * size);							     
		return hit;
	}
	
	private void handleInteractionWithPlayerFish(GameState game)
	{
		float diffX, diffY, distanceSquare;
		Fish  myFish;
		
		myFish = game.myFish.get(0);
		
		// don't interact with dead fish
		if (!myFish.alive)
			return;
		
		diffX = x -  myFish.x;
		diffY = y -  myFish.y;
		
		distanceSquare = (diffX * diffX) + (diffY * diffY);

		if (distanceSquare < FISH_INTERACTION_PROXIMITY)
		{
			float distance = (float) Math.sqrt(distanceSquare);
			ax = (float) (diffX * FISH_INTERACTION_ACCELERATION / distance);
			ay = (float) (diffY * FISH_INTERACTION_ACCELERATION / distance);			
			
			if (size > myFish.size || fishType == 's')
			{
				ax = ax * -1;
				ay = ay * -1;				
			}

			// check if the player's fish has collided with the current fish
			
			// figure out the center of the mouth of the bigger fish
			// and check distance from there instead of the center
			if (size > myFish.size || fishType == 's')
			{
				float mouthX = x + FISH_SIZE_TO_MOUTH_RATIO * size * ((float) Math.cos(angle));
				float mouthY = y + FISH_SIZE_TO_MOUTH_RATIO * size * ((float) Math.sin(angle));
				diffX = myFish.x -  mouthX;
				diffY = myFish.y -  mouthY;
				distanceSquare = (diffX * diffX) + (diffY * diffY);				
			}
			else
			{
				// Hack - to make player eat opponent fish more easily, check collision with center too
				if (distanceSquare < FISH_EAT_PROXIMITY)
				{
					game.respondToCollisionBetweenFishes(this, game);
				}				
				
				
				float mouthX = myFish.x + FISH_SIZE_TO_MOUTH_RATIO * myFish.size * ((float) Math.cos(myFish.angle));
				float mouthY = myFish.y + FISH_SIZE_TO_MOUTH_RATIO * myFish.size * ((float) Math.sin(myFish.angle));
				diffX = x -  mouthX;
				diffY = y -  mouthY;
				distanceSquare = (diffX * diffX) + (diffY * diffY);
			}
			
			if (distanceSquare < FISH_EAT_PROXIMITY)
			{
				game.respondToCollisionBetweenFishes(this, game);
			}
			
		
		}		
	}	
	
	public void givePush(GameState game, long currentTime)
	{
		// either give a push in x direction or y direction
		int dir = game.randomizer.nextInt(4);
		double pushAmount = game.randomizer.nextDouble();
		if (dir == 0)	// push in + x direction
		{
			ax = (float) pushAmount;
		}
		else if (dir == 1)	// push in -x direction
		{
			ax = (float) -pushAmount;
		}
		else if (dir == 2)	// push in + y direction
		{
			ay = (float) pushAmount;
		}
		else if (dir == 3)	// push in -y direction
		{
			ay = (float) -pushAmount;
		}
		
		nextImpulseEndTime = currentTime + GameState.FishMovementImpulseTime;		
	}	
	
	public void draw(GameState game, long currentTime)
	{
		if(alive == false)
			return;
		
		AnimationSequence currentAnime;
		
		if(eating)
		{
			// set current animation as eating
			currentAnime = fishEatAnime;	
		}
		else
		{
			// set current animation as swimming
			currentAnime = fishAnime;			
		}	
			
		// TODO: fish shape (currently assumed a square always)
				
		// figure out the frame number based on current time
		int frameNum = (int) ((currentTime / currentAnime.frameTime) % currentAnime.n);
		
		game.drawObjectRelativeToMaze(currentAnime.images[frameNum], x, y, 2  * size, 2  * size, angle);	
	}	
};
