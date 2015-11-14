package maach.game;


public class Goal 
{
	private static final float GOAL_PROXIMITY = 900;
	float x, y;
	float size;

	
	
	void draw(GameState game, long currentTime, AnimationSequence goalAnime)
	{		
		int frameNum = (int) ((currentTime / goalAnime.frameTime) % goalAnime.n);
		game.drawObjectRelativeToMaze(goalAnime.images[frameNum], x, y, 2  * size , 2  * size, 0);			
	}	
	
	boolean checkIfPlayerArrived(GameState game)
	{	
			float diffX, diffY, distance;
			Fish  myFish;
			
			myFish = game.myFish.get(0);
			
			diffX = x -  myFish.x;
			diffY = y -  myFish.y;
			
			distance = (diffX * diffX) + (diffY * diffY);
			

			if (distance < GOAL_PROXIMITY)
			{
				return true;
			}
			
			return false;			
		
	}
}
