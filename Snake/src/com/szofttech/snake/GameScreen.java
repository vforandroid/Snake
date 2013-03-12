package com.szofttech.snake;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;


public class GameScreen extends Activity {	
	private GLSurfaceView openglSurface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		openglSurface = new GLSurfaceView(this);
		openglSurface.setEGLContextClientVersion(2);
		openglSurface.setRenderer(new SnakeRenderer(getBaseContext()));

		setContentView(openglSurface);
	}
	
	
	@Override
	protected void onResume() 
	{
		// The activity must call the GL surface view's onResume() on activity onResume().
		super.onResume();
		openglSurface.onResume();
	}

	@Override
	protected void onPause() 
	{
		// The activity must call the GL surface view's onPause() on activity onPause().
		super.onPause();
		openglSurface.onPause();
	}	


	
}
