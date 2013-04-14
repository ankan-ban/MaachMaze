package maach.game;


public class AnimationSequence
{
	// handle of images in the animation sequence
	int[] images;
	
	// no of frames
	int n;
	
	float duration;		// total duration (in seconds)
	long frameTime;		// frame time in nanoseconds
	
	AnimationSequence(int num, float t)
	{
		n = num;
		images = new int [n];
		
		duration = t;
		frameTime = (long)(duration * 1000000000 / n);
	}
	
}
