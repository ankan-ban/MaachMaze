package maach.game;

public interface TwoDRenderer {
	
	// set the class which will actually draw things making use of TwoDRenderer interface
	void setFrameLooper(GameFrameLoop frameLoop);
	
	// load/initialize the image and return a handle for accessing it later 
	int loadImage(int resourceId);

	// this version also scales the image to the needed size when loading
	int loadImage(int resourceId, int width, int height);
	
	// clear the screen with the given color
	void clearScreen(float r, float g, float b, float a);
	
	// draws an image with the given translation and rotation angle
	void drawImage(int imageHandle, float x, float y, float angle);
	
	// draws an image with the given translation and rotation angle
	void drawImageScaled(int imageHandle, float x, float y, float width, float height, float angle);
	
	// draws an image with the given translation and rotation angle
	void drawImageScaledTransparent(int imageHandle, float x, float y, float width, float height, float angle, float alpha);

	// draws an image with wavy water effect
	void drawWavyImageScaled(int imageHandle, float x, float y, float width, float height, float angle, long currentTime);
	
	// draws two blended images with wavy effect on the second image: image1 + (alpha) * image2
	// xScale and yScale are the no. of times the second (transparent wavy) texture should be repeated
	void drawBlendedWavyImages(int image1, int image2, float x, float y, float width, float height, float angle, 
							   float alpha, long currentTime, float period, float magnitude, float xScale, float yScale);
		
	void onPause();
	void onResume();
	
}
