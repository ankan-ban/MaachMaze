package maach.game;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;

public class CanvasRenderer extends View implements TwoDRenderer 
{
	// list of images loaded
	ArrayList<Bitmap> 	images;
	
	// reference to the parent activity and the object that implements the game loop
	Activity 			parentActivity;
	private GameFrameLoop gameLooper;	
	
	// the canvas object used to draw
	Canvas drawingCanvas;
	
	// this should also have been a part of the interface but Java doesn't allow interfaces to have constructors
	public CanvasRenderer(Activity activity) 
	{
		super(activity);
		parentActivity = activity;
		images         = new ArrayList<Bitmap>();		
	} 	
	
	// methods to implement TwoDRenderer	
	public void setFrameLooper(GameFrameLoop frameLoop) {
		gameLooper     = frameLoop;
	}		

	// Load image from resourceId into a bitmap
	// Store the bitmap in an arrayList and return it's index as the handle
	public int loadImage(int resourceId) {
		BitmapFactory.Options bfOptions = new BitmapFactory.Options();
		bfOptions.inDither = true;                      
	    bfOptions.inPurgeable = true;                   // Tell to gc that whether it needs free memory, the Bitmap can be cleared
	    bfOptions.inInputShareable = true;              // Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
	    bfOptions.inTempStorage = new byte[32 * 1024]; 
	    
		Bitmap b = BitmapFactory.decodeResource(parentActivity.getResources(), resourceId, bfOptions);
		int handle = images.size();
		images.add(b);
		return handle;
	}

	// same as above, but allows to specify width and height
	public int loadImage(int resourceId, int width, int height) {
		Bitmap b = BitmapFactory.decodeResource(parentActivity.getResources(), resourceId);
		b = Bitmap.createScaledBitmap(b, width, height, true);
		int handle = images.size();
		images.add(b);
		return handle;
	}	
	
	// clears the screen with the given color
	public void clearScreen(float r, float g, float b, float a) 
	{
		drawingCanvas.drawARGB((int)a, (int)r, (int)g, (int)b);		
	}

	// draws the image at the specified position and angle
	public void drawImage(int imageHandle, float x, float y, float angle) 
	{
		Bitmap b = images.get(imageHandle);
		
		Matrix m = new Matrix();
		m.postTranslate(-b.getWidth()/2.0f, -b.getHeight()/2.0f);
		m.postRotate(angle);
		m.postTranslate( x, y);
		
		drawingCanvas.drawBitmap(b, m, null);
	}

	// draws the image at the specified position and angle
	public void drawImageScaled(int imageHandle, float x, float y, float width, float height, float angle) 
	{
		Bitmap b = images.get(imageHandle);
		
		Matrix m = new Matrix();
		m.postTranslate(-width/2.0f, -height/2.0f);
		m.postRotate(angle);
		m.postTranslate( x, y);
		m.preScale(width/b.getWidth(), height/b.getHeight());
		drawingCanvas.drawBitmap(b, m, null);
	}
	
	public void drawImageScaledTransparent(int imageHandle, float x, float y, float width, float height, float angle, float alpha) 
	{
		Paint p = new Paint();

		//Set transparency roughly at 50%
		p.setAlpha(125);
		// use this paint object to draw the bitmap (3rd parameter.. TODO, check if this works)
		
		// currently not supported. call the regular draw method
		drawImageScaled(imageHandle, x, y, width, height, angle);
	}
	
	
	public void drawWavyImageScaled(int imageHandle, float x, float y, float width, float height, float angle, long currentTime)
	{
		// not supported. call the regular draw method
		drawImageScaled(imageHandle, x, y, width, height, angle);
	}	

	public void drawBlendedWavyImages(int image1, int image2, float x, float y, float width, float height, 
									  float angle, float alpha, long currentTime, float period, 
									  float magnitude, float xScale, float yScale) 
	{
		// not supported. call the regular draw method for drawing only the first image
		drawImageScaled(image1, x, y, width, height, angle);
	}	
	
	// methods to extend from View class
	protected void onDraw(Canvas canvas) 
	{
		
		drawingCanvas = canvas;
		
		// the gameLooper object will loop over all objects and call the functions implemented above to render the scene
		gameLooper.render(this);
		invalidate();
		
	}


	// not needed for canvas
	public void onPause() {
		// TODO Auto-generated method stub
		
	}

	public void onResume() {
		// TODO Auto-generated method stub
		
	}

}

