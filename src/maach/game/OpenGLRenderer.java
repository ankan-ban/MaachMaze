package maach.game;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;

class ImageInfo
{
	// openGL texture handle
	public int handle;
	public int resourceId;

	// default dimensions of the image
	public int width, height;
	
	ImageInfo(int r, int h, int w, int he)
	{
		resourceId = r;
		handle = h;
		width = w;
		height = he;
	}
};


public class OpenGLRenderer extends GLSurfaceView implements Renderer, TwoDRenderer 
{

	// reference to the parent activity and the object that implements the game loop
	Activity 			  parentActivity;
	private GameFrameLoop gameLooper = null;
	private ArrayList<ImageInfo> images;
	private boolean initialized = false;
	
	public OpenGLRenderer(Activity activity) 
	{
		super(activity);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		
		images  = new ArrayList<ImageInfo>();
		parentActivity = activity;
	}


	//
	// methods to implement android.opengl.GLSurfaceView.Renderer
	//
	public void onDrawFrame(GL10 gl) {
		if(gameLooper != null)
			gameLooper.render(this);
	}

	public void onSurfaceChanged(GL10 unused, int width, int height) 
	{
		halfScreenWidth = width / 2.0f;
		halfScreenHeight = height / 2.0f;		
	}

	public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
		oglInit();
		createPendingTextures();
		initialized = true;
	}
	
	
	//
	// internal methods
	//
	
	private void createPendingTextures() 
	{
		for(int i=0; i < images.size(); i++)
		{
			images.get(i).handle = loadTexture(parentActivity, images.get(i).resourceId);
		}
	}

	// vertex buffers
	private FloatBuffer boxVB, boxTexVB;
	
	private float halfScreenWidth, halfScreenHeight;	// for easier calculations
    private float[] matrix = null;								// matrix for transformations
    
	private int matrixHandle;
	
	// old stuff that we tried but no longer used
	// private int timeUniformHandle;
	// private int waveScaleHandle;
	// private int waterEffectProgram;
	// private int twoImageBlendProgram;
	
	private int alphaUniformHandle;
	
	private int normalProgram;
	private int alphaProgram;
	private int waterBlendEffectProgram;
	
	private int timeHandleWaterBlendProgram;
	private int waveScaleHandleWaterBlendProgram;
	private int alphaUniformHandleWaterBlendProgram;	
	private int xScaleUniformHandleWaterBlendProgram;
	private int yScaleUniformHandleWaterBlendProgram;	
	
	private int firstTexHandle;
	private int secondTexHandle;
	private int thirdTexHandle;	
	
	private int sineTexHandle;
	
	private void oglInit() {
		
		initVertexBuffers();
		matrix = new float[16];
		
		  // Enable texture mapping
	    GLES20.glEnable(GLES20.GL_TEXTURE_2D);

	    // create the shaders
	    int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderProgram);
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderProgram);
		int alphaShader    = loadShader(GLES20.GL_FRAGMENT_SHADER, alphaEffectShader);
		int waterBlendShader = loadShader(GLES20.GL_FRAGMENT_SHADER, waterBlendEffectShaderOpt2);
		int waterBlendVS     = loadShader(GLES20.GL_VERTEX_SHADER, waterBlendEffectVertexShader);

		
		// TODO: important: different uniform handles for different shader programs (otherwise we rely on undefined behaviour causing lot of pain!)
		normalProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(normalProgram, vertexShader);
		GLES20.glAttachShader(normalProgram, fragmentShader);
		GLES20.glLinkProgram(normalProgram);
		
	
		alphaProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(alphaProgram, vertexShader);
		GLES20.glAttachShader(alphaProgram, alphaShader);
		GLES20.glLinkProgram(alphaProgram);
		alphaUniformHandle = GLES20.glGetUniformLocation(alphaProgram, "alpha");
				
		waterBlendEffectProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(waterBlendEffectProgram, waterBlendVS);
		GLES20.glAttachShader(waterBlendEffectProgram, waterBlendShader);
		GLES20.glLinkProgram(waterBlendEffectProgram);
		
        timeHandleWaterBlendProgram = GLES20.glGetUniformLocation(waterBlendEffectProgram, "time");
        waveScaleHandleWaterBlendProgram   = GLES20.glGetUniformLocation(waterBlendEffectProgram, "waveScale");
        alphaUniformHandleWaterBlendProgram = GLES20.glGetUniformLocation(waterBlendEffectProgram, "alpha");
        
        xScaleUniformHandleWaterBlendProgram = GLES20.glGetUniformLocation(waterBlendEffectProgram, "xScale");
        yScaleUniformHandleWaterBlendProgram = GLES20.glGetUniformLocation(waterBlendEffectProgram, "yScale");

        
		int vertexHandle = GLES20.glGetAttribLocation(normalProgram, "vPosition");	
		int textureCoordHandle = GLES20.glGetAttribLocation(normalProgram, "vTexture");
		
		int textureHandle = GLES20.glGetUniformLocation(normalProgram, "uTexture");
		
		firstTexHandle = GLES20.glGetUniformLocation(waterBlendEffectProgram, "firstTex");
		secondTexHandle = GLES20.glGetUniformLocation(waterBlendEffectProgram, "secondTex");
		thirdTexHandle = GLES20.glGetUniformLocation(waterBlendEffectProgram, "sineTexture");
		
        matrixHandle = GLES20.glGetUniformLocation(normalProgram, "MVPMatrix");     
		
        // old stuff that we tried but not no longer used

        //int waterShader    = loadShader(GLES20.GL_FRAGMENT_SHADER, waterEffectShader);
		//int waterShaderOpt = loadShader(GLES20.GL_FRAGMENT_SHADER, waterEffectShader);
		//int twoBlendShader = loadShader(GLES20.GL_FRAGMENT_SHADER, twoImageBlendShader);
        
		// twoImageBlendProgram = GLES20.glCreateProgram();
		// GLES20.glAttachShader(twoImageBlendProgram, vertexShader);
		// GLES20.glAttachShader(twoImageBlendProgram, twoBlendShader);
		// GLES20.glLinkProgram(twoImageBlendProgram);
		// alphaUniformHandle = GLES20.glGetUniformLocation(twoImageBlendProgram, "alpha");
        
        // timeUniformHandle = GLES20.glGetUniformLocation(waterEffectProgram, "time");	// hopefully locations of other uniforms are same
        // waveScaleHandle   = GLES20.glGetUniformLocation(waterEffectProgram, "waveScale");
        // int sineTexLoc 	  = GLES20.glGetUniformLocation(waterEffectProgram, "sineTexture");
        
		// waterEffectProgram = GLES20.glCreateProgram();
		// GLES20.glAttachShader(waterEffectProgram, vertexShader);
		// GLES20.glAttachShader(waterEffectProgram, waterShader);
		// GLES20.glAttachShader(waterEffectProgram, waterShaderOpt);
		// GLES20.glLinkProgram(waterEffectProgram);        
	    
        // create the sine texture (used as lookup table) and bind it to slot 2
        sineTexHandle = createSineTexture();

        
		// Set the active texture unit to texture unit 0.
	    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	 
	    // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
	    GLES20.glUniform1i(textureHandle, 0);
	    
 
	    
	    // set vertex buffers
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 12, boxVB);
        GLES20.glEnableVertexAttribArray(vertexHandle);
    
        boxTexVB.position(0);
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, boxTexVB);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);
        
        // enable alpha blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);        
        
	}
	
	void initVertexBuffers()
	{
		float[] positions = {
							 -0.5f,	-0.5f,	0f,
							 -0.5f,	0.5f,	0f,
							  0.5f,	-0.5f,	0f,
						
							  0.5f, 0.5f,	0f,
							 -0.5f, 0.5f,	0f,
							  0.5f, -0.5f,	0f,				
				            };
		
		float[] texCoords = {
							0f, 1f,
							0f, 0f,
							1f, 1f,
						
							1f, 0f,
							0f, 0f,
							1f, 1f				
				           };
		
	
		ByteBuffer byteBuffer=ByteBuffer.allocateDirect(positions.length*4);
		byteBuffer.order(ByteOrder.nativeOrder());
		boxVB = byteBuffer.asFloatBuffer();
		boxVB.put(positions);
		boxVB.position(0);
		
		
		boxTexVB = ByteBuffer.allocateDirect(texCoords.length * 4)
								  .order(ByteOrder.nativeOrder()).asFloatBuffer();
		boxTexVB.put(texCoords).position(0);
		
		
	}
	

    public static int loadTexture(final Context context, final int resourceId)
    {
        final int[] textureHandle = new int[1];
     
        GLES20.glGenTextures(1, textureHandle, 0);
     
        if (textureHandle[0] != 0)
        {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Config.ARGB_8888;
            options.inScaled = false;   // No pre-scaling
            options.inPurgeable = true; // saves memory
     
            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
     
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
     
            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            
	    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);	    	
            
     
            // Load the bitmap into the bound texture.
            
            // texImage2D is buggy - it messes up the alpha
            //GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            
    		int pixels[] = new int[bitmap.getWidth() * bitmap.getHeight()]; 
    		bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());        
    		
    		// convert ARGB to RGBA
    		for (int i=0; i<pixels.length; i++) {
    		    int argb = pixels[i];
    		    pixels[i] = argb & 0xff00ff00 | ((argb & 0xff) <<16) | ((argb >> 16) & 0xff);
    		}
    		
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.getWidth(), bitmap.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels)); 
            
            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }
     
        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }
     
        return textureHandle[0];
    }
	
	
	private int loadShader(int typeOfShader, String nameOfShader)
	{
		int shader=GLES20.glCreateShader(typeOfShader);
		GLES20.glShaderSource(shader, nameOfShader);
		GLES20.glCompileShader(shader);
		// for debugging
		String str;
		str = GLES20.glGetShaderInfoLog(shader);
		return shader;
	}
	
    private final String vertexShaderProgram = 
            "attribute vec4 vPosition; 				\n" +
            "attribute vec2 vTexture;               \n"	+	
            "uniform mat4 MVPMatrix;  				\n" +
            "varying vec2 texCoord;			        \n" +
            "void main(){                           \n" +
            "texCoord=vTexture;						\n" +
            "gl_Position = MVPMatrix * vPosition;  \n" +
            "}                        			    \n";
        
    private final String fragmentShaderProgram = 
            "precision mediump float;  		   \n" +
            "uniform sampler2D uTexture;	   \n" +	
            "varying vec2 texCoord;		       \n" +		
            "void main(){                      \n" +
            "gl_FragColor = texture2D(uTexture, texCoord); \n" +
            "}                         \n";	

    private final String alphaEffectShader = 
            "precision mediump float;  		   \n" +
            "uniform sampler2D uTexture;	   \n" +	
            "varying vec2 texCoord;		       \n" +
            "uniform float alpha;	           \n" +             
            "void main(){                      \n" +
            "gl_FragColor = texture2D(uTexture, texCoord); \n" +
            "gl_FragColor.w = alpha; 	\n" +
            "}                          \n";	
    
    
    // unused old shaders
    
    /*
    private final String twoImageBlendShader = 
            "precision mediump float;  		   \n" +
            "uniform sampler2D firstTex;	   \n" +
            "uniform sampler2D secondTex;	   \n" +            		
            "varying vec2 texCoord;		       \n" +
            "uniform float alpha;	           \n" +             
            "void main(){                      \n" +
            "gl_FragColor = (alpha) * texture2D(firstTex, texCoord) + (1.0 - alpha) * texture2D(secondTex, texCoord); \n" +
            // "gl_FragColor.r = 1.0; \n" +  // - for testing
            "}                          \n";	
    		
    private final String waterBlendEffectShader = 
            "precision mediump float;  		   \n" +
            "uniform sampler2D firstTex;	   \n" +
            "uniform sampler2D secondTex;	   \n" +            		
            "varying vec2 texCoord;		       \n" +
            "uniform float time;	           \n" + 
            "uniform float waveScale;          \n" +              
            "uniform float alpha;	           \n" +             
            "void main(){                      \n" +
            "  vec2 coord = texCoord;\n"+ 
            "  coord.x += sin(time + texCoord.x*10.0) * waveScale; \n" +
            "  coord.y += cos(time + texCoord.y*10.0) * waveScale; \n" +            
            "  gl_FragColor = (alpha) * texture2D(firstTex, texCoord) + (1.0 - alpha) * texture2D(secondTex, coord); \n" +
            "}                          \n";	    		
    
    private final String waterEffectShader = 
            "precision mediump float;  		   \n" +
            "uniform sampler2D uTexture;	   \n" +	
            "varying vec2 texCoord;		       \n" +			
            "uniform float time;	           \n" + 
            "uniform float waveScale;          \n" +            
            "void main(){                      \n" +
            "  vec2 coord = texCoord;\n"+ 
            "  coord.x += sin(time + texCoord.x*10.0) * waveScale; \n" +
            "  coord.y += cos(time + texCoord.y*10.0) * waveScale; \n" +
            "  gl_FragColor = texture2D(uTexture, coord); \n" +
            "}                         \n";	
    
    
    // optimized version of the above shader using lookup table for sin/cos
    private final String waterEffectShaderOpt = 
            "precision mediump float;  		   \n" +
            "uniform sampler2D uTexture;	   \n" +
            "uniform sampler2D sineTexture;    \n" +		// lookup table for sine range 0-1 corrosponds to 0 to 2PI, values 0-1 corrosponds to values from -1 to 1
            "varying vec2 texCoord;		       \n" +			
            "uniform float time;	           \n" + 
            "uniform float waveScale;          \n" +            
            "void main(){                      \n" +
            "  vec2 coord = texCoord; \n"+
            "  vec2 coordX, coordY; \n" +
            "  coordX.y = coordY.y = 0.5; \n" +
            "  coordX.x = time + texCoord.x; \n" +
            "  coordY.x = 0.25 + time + texCoord.y; \n" + // 0.25 = pi/2, cos(x) = sin (x+pi/2)
            
            "  coord.x += (2.0 * texture2D(sineTexture, coordX) - 1.0).x * waveScale; \n" + 			// use lookup table to get value of sine function
            "  coord.y += (2.0 * texture2D(sineTexture, coordY) - 1.0).x * waveScale; \n" +		    // same lookup table for cosine just from different location
            //"  coord.x += sin(time + texCoord.x*10.0) * waveScale; \n" +
            //"  coord.y += cos(time + texCoord.y*10.0) * waveScale; \n" +
            "  gl_FragColor = texture2D(sineTexture, coord); \n" +
            "}                         \n";	
        
    
    private final String waterBlendEffectShaderOpt = 
            "precision mediump float;  		   \n" +
            "uniform sampler2D firstTex;	   \n" +
            "uniform sampler2D secondTex;	   \n" +          
            "uniform sampler2D sineTexture;    \n" +		// lookup table for sine range 0-1 corrosponds to 0 to 2PI, values 0-1 corrosponds to values from -1 to 1            
            "varying vec2 texCoord;		       \n" +
            "uniform float time;	           \n" + 
            "uniform float waveScale;          \n" +              
            "uniform float alpha;	           \n" +             
            "void main(){                      \n" +
            //"  vec2 coord = texCoord;\n"+ 
            //"  coord.x += sin(time + texCoord.x*10.0) * waveScale; \n" +
            //"  coord.y += cos(time + texCoord.y*10.0) * waveScale; \n" +
            "  vec2 coord = texCoord; \n"+
            "  vec2 coordX, coordY; \n" +
            "  coordX.y = coordY.y = 0.5; \n" +
            "  coordX.x = time + texCoord.x; \n" +
            "  coordY.x = 0.25 + time + texCoord.y; \n" + // 0.25 = pi/2, cos(x) = sin (x+pi/2)
            
            // for testing
            
            // " gl_FragColor.r = texture2D(sineTexture, coordX).x; \n" +
            // " gl_FragColor.g = texture2D(sineTexture, coordY).x; \n" +
            // " gl_FragColor.b = 0.0; \n" +
            // " gl_FragColor.a = 1.0; \n" +
                     
            
            "  coord.x += (texture2D(sineTexture, coordX) - 0.5).x * waveScale; \n" + 			// use lookup table to get value of sine function
            "  coord.y += (texture2D(sineTexture, coordY) - 0.5).x * waveScale; \n" +		    // same lookup table for cosine just from different location
            
            "  gl_FragColor = texture2D(firstTex, texCoord) + (alpha) * texture2D(secondTex, coord); \n" +
            
            "}       \n";	    		
    */
    
    // moves some of the work (lookup table tex-coord calculation) to vertex shader
    // (no difference vs doing that in PS)
    private final String waterBlendEffectVertexShader = 
            "attribute vec4 vPosition; 				\n" +
            "attribute vec2 vTexture;               \n"	+	
            "uniform mat4 MVPMatrix;  				\n" +
            "uniform float time;	           		\n" + 
            "uniform float xScale;					\n" +
            "uniform float yScale;					\n" +            
            "varying vec2 texCoord;			        \n" +
            "varying vec2 sineLookupTexCoord;       \n" + 
            "varying vec2 secondTexCoord;       	\n" + 
            "void main(){                           \n" +
            "texCoord = vTexture;					\n" +
            "secondTexCoord = vTexture * vec2(xScale, yScale); \n" + 
            "sineLookupTexCoord.x = time + secondTexCoord.x * 2.0;\n" + 
            "sineLookupTexCoord.y = 0.25 + time + secondTexCoord.y * 2.0; \n" +            // 0.25 = pi/2, cos(x) = sin (x+pi/2)
            "gl_Position = MVPMatrix * vPosition;  \n" +
            "}       \n";
    
    private final String waterBlendEffectShaderOpt2 = 
            "precision mediump float;  		   \n" +
            "uniform sampler2D firstTex;	   \n" +
            "uniform sampler2D secondTex;	   \n" +          
            "uniform sampler2D sineTexture;    \n" +		// lookup table for sine range 0-1 corrosponds to 0 to 2PI, values 0-1 corrosponds to values from -1 to 1            
            "varying vec2 texCoord;		       \n" +
            "varying vec2 sineLookupTexCoord;  \n" + 		// tex-coords to use when texturing from the lookup table containing sine values
            "varying vec2 secondTexCoord;      \n" +        // tex-coords for the wavy texture
            "uniform float waveScale;          \n" +              
            "uniform float alpha;	           \n" +             
            "void main(){                      \n" +
            "  vec2 coordX, coordY; \n" +
            
            "  coordX.y = coordY.y = 0.5; \n" +
            "  coordX.x = sineLookupTexCoord.x; \n" +
            "  coordY.x = sineLookupTexCoord.y; \n" + 

            "  vec2 coord = secondTexCoord; \n"+
            "  coord.x += (texture2D(sineTexture, coordX) - 0.5).x * waveScale; \n" + 			// use lookup table to get value of sine function
            "  coord.y += (texture2D(sineTexture, coordY) - 0.5).x * waveScale; \n" +		    // same lookup table for cosine just from different location
            
            "  gl_FragColor = texture2D(firstTex, texCoord) + (alpha) * texture2D(secondTex, coord); \n" +
            
            "}       \n";	    
    
    int createSineTexture()
    {
    	final int TEX_SIZE = 512;
    	byte []image = new byte[TEX_SIZE];
    	
    	// generate the lookup table values
    	for(int i = 0; i < TEX_SIZE; i++)
    	{
    		double x = i * 2.0 * Math.PI / TEX_SIZE;
    		double y = Math.sin(x);
    		// map -1 to 1 to 0 to 1
    		y = y / 2 + 0.5;
    		image[i] = (byte) (y * 255.0);
    	}

    	ByteBuffer buf = ByteBuffer.wrap(image);;

    	
    	
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
     
        if (textureHandle[0] != 0)
        {
        	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        	
            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            
            // set addressing mode (wrap)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            // load the lookup table into the texture
            // hopefully GL_LUMINANCE is single component format (L8 ?) - Yes it is!
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, TEX_SIZE, 1, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, buf);        	
        }
    	
    	return textureHandle[0];
    }
    
    
    
    //
	// methods to implement TwoDRenderer
	//
    
	public void setFrameLooper(GameFrameLoop frameLoop) {
		gameLooper     = frameLoop;
	}

	public int loadImage(int resourceId) 
	{
		return loadImage(resourceId, 128, 128);	// TODO: default image size 
	}

	public int loadImage(int resourceId, int width, int height) 
	{
		// load the texture into openGL and save openGL texture handle
		int oglHandle = 0;
		
		// if the texture is created before OGL is initialized, we will create all pending textures later
		if (initialized)
		{
			oglHandle = loadTexture(parentActivity, resourceId);
		}
		
		ImageInfo image = new ImageInfo(resourceId, oglHandle, width, height);

		// add the image info into our ArrayList and return it's index as the handle
		int handle = images.size();		
		images.add(image);
		return handle;
	}

	public void clearScreen(float r, float g, float b, float a) {
		GLES20.glClearColor(r/255.0f, g/255.0f, b/255.0f, a/255.0f);		
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
	}

	public void drawImage(int imageHandle, float x, float y, float angle) 
	{
		drawImageScaled(imageHandle, x, y, images.get(imageHandle).width, images.get(imageHandle).height, angle);
	}
	
	private void drawImageCommon(int imageHandle, float x, float y, float width, float height, float angle)
	{
		if (imageHandle == -1)
		{
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		}
		else
		{
			ImageInfo image = images.get(imageHandle);

			// Bind the texture/image
	    	GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, image.handle);
		}
	    
	    // the image width given is in pixels but openGL draw will draw from -0.5 to 0.5, covering half width or height
	    float widthScale  = width/halfScreenWidth;
	    float heightScale = height/halfScreenHeight;
	    
	    // the x, y co-ordinates are given in screen space (from top to bottom, from left to right)
	    // but openGL coordinate system has origin at center and ranges from -1 to 1 in either direction
	    float xOffset =  x / halfScreenWidth  - 1.0f;
	    float yOffset = -y / halfScreenHeight + 1.0f;
	    
        // update rotation, translation, scaling information into the matrix
        Matrix.setIdentityM(matrix, 0);
		Matrix.translateM(matrix, 0, xOffset, yOffset, 0);
		Matrix.scaleM(matrix, 0, widthScale, heightScale, 1);        
		Matrix.rotateM(matrix, 0, -angle, 0, 0, 1);
		GLES20.glUniformMatrix4fv(matrixHandle, 1, false, matrix, 0);
		
		// trigger the draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);		
	}

	public void drawImageScaled(int imageHandle, float x, float y, float width, float height, float angle) 
	{
	    // set the regular program (vertex and pixel shader)
        GLES20.glUseProgram(normalProgram);

		drawImageCommon(imageHandle, x, y, width, height, angle);
	}
	
	public void drawImageScaledTransparent(int imageHandle, float x, float y, float width, float height, float angle, float alpha) 
	{
	    // set the alpha program (vertex and pixel shader)
        GLES20.glUseProgram(alphaProgram);

        // update the alpha value (uniform in the pixel shader)
        GLES20.glUniform1f(alphaUniformHandle, alpha);

		drawImageCommon(imageHandle, x, y, width, height, angle);		
	}
	
	public void drawBlendedWavyImages(int image1, int image2, float x, float y,
			float width, float height, float angle, float alpha, long currentTime, 
			float period, float magnitude, float xScale, float yScale) {
		
		
	    // set the image blend program (vertex and pixel shader)
        GLES20.glUseProgram(waterBlendEffectProgram);

        // set the no. of times the wavy texture is to be repeated in either direction
        GLES20.glUniform1f(xScaleUniformHandleWaterBlendProgram, xScale);
        GLES20.glUniform1f(yScaleUniformHandleWaterBlendProgram, yScale);        
        
        /*
        final float period    = 2.0f;	// time period in seconds (adjust this to make the motion slow or fast)
        final float magnitude = 0.02f;	// magnitude of the effect
        */
        
        // update the uniform for time
        final long periodInNs = (long) (period * 1000000000l); 
        float time = ((float) (currentTime % periodInNs)) / periodInNs;	
        
        //time = (float) (time * 2.0 * Math.PI);			// this probably isn't needed if the lookup table is between 0 and 1 (instead of 0 to 2_pi)
        GLES20.glUniform1f(timeHandleWaterBlendProgram, time);

        // set the amount of effect to add
        GLES20.glUniform1f(waveScaleHandleWaterBlendProgram, magnitude);
        
        // update the alpha value (uniform in the pixel shader)
        GLES20.glUniform1f(alphaUniformHandleWaterBlendProgram, alpha);
        
        // set the second texture at slot 1
		ImageInfo image = images.get(image2);        
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
	    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, image.handle);
	    GLES20.glUniform1i(secondTexHandle, 1);	    
	    
		// hack for start screen		
	    if (xScale != 1.0f || yScale != 1.0f)
	    {
	    	// set addressing mode (mirror) only when we need to repeat the texture
	    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_MIRRORED_REPEAT);
	    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT);
	    }
	    else
	    {
	    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	    	GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);	    	
	    }
	    
	    // set the sine texture at slot 2
	    GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
	    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sineTexHandle);
	    GLES20.glUniform1i(thirdTexHandle, 2);
	    
	    
	    // set active slot back to 0
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	    GLES20.glUniform1i(firstTexHandle, 0);	    
        	    
		
		drawImageCommon(image1, x, y, width, height, angle);		        
        
	}	
	
	public void drawWavyImageScaled(int imageHandle, float x, float y, float width, float height, float angle, long currentTime)
	{
		

        final float period    = 2.0f;	// time period in seconds (adjust this to make the motion slow or fast)
        final float magnitude = 0.02f;	// magnitude of the effect
        
        // use the new optimized version
        drawBlendedWavyImages(-1, imageHandle, x, y,
    			width, height, angle, 1.0f, currentTime, 
    			period, magnitude, 1.0f, 1.0f);        
        
        // old code:
        
        /*
	    // set the water effect program (vertex and pixel shader)
        GLES20.glUseProgram(waterEffectProgram);
        
        // update the uniform for time
        final long periodInNs = (long) (period * 1000000000l); 
        float time = ((float) (currentTime % periodInNs)) / periodInNs;	
        
        time = (float) (time * 2.0 * Math.PI);			// this probably isn't needed if the lookup table is between 0 and 1 (instead of 0 to 2_pi)
        GLES20.glUniform1f(timeUniformHandle, time);

        // set the amount of effect to add
        GLES20.glUniform1f(waveScaleHandle, magnitude);
                
		drawImageCommon(imageHandle, x, y, width, height, angle);
		*/		
	}
	
}
