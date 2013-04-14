package maach.game;

public class Settings
{
	// True - use touch input to move the fish
	// False  - use tilt (accelerometer) input to move the fish (default)
	static boolean useTouchInput = false;
	
	// True - use openGL renderer (default)
	// False  - use canvas renderer 
	static boolean useOpengl = true;
	
	static boolean soundsOn = true;	
	
	// enables or disables the water effect
	static boolean waterOn = true;
	
	// true if the system has opengl es 2.0 support
	static boolean hasOpenGLSupport = true;
	
	// neutral position acceleration values
	static float neutral_Ax, neutral_Ay;
	
	// accelerometer sensitivity
	static float sensitivity;
}