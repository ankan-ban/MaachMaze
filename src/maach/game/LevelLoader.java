package maach.game;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;

//// level loader class reads a level file and fills it into our data structures (GameState object)

//the format of the level file is as follows:
//###############################
//#   p        5        #       #
//#       #             #       #
//#       #             ######  #
//#      2#               9     #
//###############################
//
//# 	-   wall element (of size block_size x block_size)
//p 	-   player's fish
//0-9 	-   enemy fish
//s    -   shark

class LevelLoader
{
	private static final int max_cols = 500;
	private static final int max_rows = 500;
	private Activity theActivity = null;
	
	private char[][] maze;
	private int rows = 0, cols = 0;
	
	GameState game = null;
	
	LevelLoader(Activity act)
	{
		theActivity = act;
	}
	
	public void loadLevel(GameState gameState, int levelFileId)
	{
		rows = 0;
		cols = 0;
		
		maze = new char[max_rows][max_cols];
		
		game = gameState;
		InputStream inputStream = theActivity.getResources().openRawResource(levelFileId);    
	    try {
	    	readFile(inputStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    parseLevel(); 
	}
	
	// reads level file and stores in the 2d matrix called maze
	// also updates member variables - 'rows' and 'cols 
	private void readFile(InputStream inputStream) throws IOException								
	{      	
	     char ch = 0;
	     int input=0;
	     int row=0, column=0;
		   
	     while(true)
	     {
	     	input = inputStream.read();					
				
	     	if(input == -1)		// -1 if EOF is reached
	     		break;
	     		
	     		
	     	ch = (char) input;
	     	
	     	if(ch == '\r')
	     	{
	     		// go to next row in the matrix
	     		row = row + 1;
	     		cols = max(cols, column); 
	     		column = 0;
	     		
	     		input=inputStream.read();			// read the newline '\n' following the '\r'
	     	}
	     	else if (ch == '\n')
	     	{
	     		// go to next row in the matrix
	     		row = row + 1;   
	     		cols = max(cols, column);        		
	     		column = 0;
	     	}
	     	else
	     	{
	     		maze[row][column] = ch;        		
	     		column = column + 1;
	     	}    	        			        	
	     }
	     
	     rows = row;
	 }

	private int max(int x, int y) {
	 	if(x > y)
	 		return x;
	 	
	 	return y;
	 }

 
	private void addHorizontalWalls()
	{
		// temp object to store the current wall
		Wall wall = new Wall();
		
		assert(rows < max_rows - 1);	// because the if check below tries accessing the next item also
		
	 	for(int i = 0; i < rows; i++)
	 	{    		
	 		for(int j = 0; j < cols; j++)
	 		{
	 			if(maze[i][j]== '#' && maze[i][j+1] == '#')			// at least 2 consecutive blocks
	 			{
	 				wall.x      =    j * GameState.block_size;		// the beginning of the wall x
					wall.width  =    GameState.block_size;			// we will add more to this below	
						
					wall.y      =    i * GameState.block_size;		// the beginning of the wall y
					wall.height =    GameState.block_size;			// horizontal walls are always block_size high
						
	 				while(true)
	 				{
	 					j = j + 1;
	 					if(maze[i][j] != '#')						// if wall ends here 
	 					{
	 						// add this wall the list of walls    						
	 						game.horizontalWalls.add(wall);
	 						
	 						// create a new wall object
	 						wall = new Wall();			
	 						break;
	 					}
	 					else											
	 					{
	 						wall.width = wall.width + GameState.block_size;		//increase the end_wall    						
	 					}    					
	 				} // while ...
	 				
	 			}	// if (maze[i][j]== '#' ...
	 			
	 		}	// for j...
	 	}	// for i...	  
 	}
 
	private void addVerticalWalls()    
	{
		// temp object to store the current wall
		Wall wall = new Wall();
		assert(cols < max_cols - 1);	// because the if check below tries accessing the next item also
	   
	 	for(int i= 0; i < cols; i++)
	 	{    		
	 		for(int j = 0; j < rows; j++)
	 		{
	 			if(maze[j][i] == '#' && maze[j+1][i] == '#')			// at least 2 consecutive blocks		
	 			{
	 				wall.x      =    i * GameState.block_size;			// the beginning of the wall x
					wall.width  =    GameState.block_size;			    // vertical walls are always block_size wide 
						
					wall.y      =    j * GameState.block_size;			// the beginning of the wall y
					wall.height =    GameState.block_size;			    // we will add more to this below ...
						
	 				while(true)
	 				{		
	 					j = j + 1;   					
	 					if(maze[j][i] != '#')							//if wall ends here
	 					{    						
	 						// add this wall the list of walls    						
	 						game.verticalWalls.add(wall);
	 							
	 						// create a new wall object    						
								wall = new Wall();    						
								break; 		    				
	 					}
	 					else											
	 					{
	 						wall.height = wall.height + GameState.block_size;	//increase the end_wall    						
	 					}    					
	 				} // while ...
	 				
	 			}	// if (maze[i][j]== '#' ...
	 			
	 		}	// for j...
	 	}	// for i...	   	
	}
 
	private void addFish()
	{
	 	for(int i = 0; i < rows; i++)
	 	{
	 		for(int j = 0 ; j < cols; j++)
	 		{
	 			if(maze[i][j] >= '0' && maze[i][j] <= '9' || maze[i][j] == 's')	      //variety of enemy fish type
	 			{
	 				// create a new fish object    				
	 				Fish fish = new Fish();    	
	 				
	 				fish.x = (j * GameState.block_size) + GameState.block_size/2.0f;
	 				fish.y = (i * GameState.block_size) + GameState.block_size/2.0f;
	 				fish.fishType = maze[i][j];
	 				
	 				// add the fish to the list of fishes
	 				game.otherFish.add(fish);
					  				
	 			} 
	 			if(maze[i][j]=='p')												//your fish type is 3
	 			{
	 				// create a new fish object    				
	 				Fish fish = new Fish();
	 				fish.x = (j * GameState.block_size) + GameState.block_size/2.0f;
	 				fish.y = (i * GameState.block_size) + GameState.block_size/2.0f;
	 				
	 				fish.fishType=maze[i][j];    						
	 				game.myFish.add(fish);
	 			}   			
	 		}    
	 	}  	
	}
	
	private void addGoal()
	{
		for(int i = 0; i < rows; i++)
	 	{
	 		for(int j = 0 ; j < cols; j++)
	 		{
	 			if(maze[i][j] == 'g')	      //the goal
	 			{
	 				
	 				game.goal.x = j * GameState.block_size + GameState.block_size/2.0f;
	 				game.goal.y = i * GameState.block_size + GameState.block_size/2.0f;
	 				
	 				//as only one goal return on 'g' , ignore other g
	 				return;											 						 				
	 			}
	 		}	
	 	}
	}
 
 
	// reads the maze and fills up gameState object    
	private void parseLevel()
	{
		addHorizontalWalls ();
		addVerticalWalls();
		addFish();
		addGoal();
		
		// update the size of the maze
		game.mazeWidth  = cols * GameState.block_size;
		game.mazeHeight = rows * GameState.block_size;		
	 }
};
