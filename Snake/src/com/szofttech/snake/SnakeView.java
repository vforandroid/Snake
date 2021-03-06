package com.szofttech.snake;

import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class SnakeView extends GLSurfaceView {
	private final Game game;
	private final PointF downPoint;
	private static final float TOUCH_THRESHOLD=1.5f;
	private static final int MIN_DISTANCE=10;
	
	public SnakeView(final Game game) {
		super(game.context);
		this.game=game;
		downPoint=new PointF();
		
		setEGLContextClientVersion(2);
		setRenderer(game.renderer);
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		float x = e.getX();
	    float y = e.getY();
	    
	    switch (e.getAction()) {
		    case MotionEvent.ACTION_MOVE:
	   			float dx=downPoint.x-x;
	   			float dy=downPoint.y-y;
	   			
	   			if (Math.abs(dx)<MIN_DISTANCE && Math.abs(dy)<MIN_DISTANCE)
	   				return true;
	   			
	   			boolean swiped=false;
	   			if (Math.abs(dx/dy)>TOUCH_THRESHOLD){
	   				if (dx>0)
	   					game.networkManager.putLocalDirection(Snake.Direction.UP);
	   				else
	   					game.networkManager.putLocalDirection(Snake.Direction.DOWN);
	   				
	   				swiped=true;
	   			} else if (Math.abs(dy/dx)>TOUCH_THRESHOLD){
	   				if (dy>0)
	   					game.networkManager.putLocalDirection(Snake.Direction.RIGHT);
	   				else
	   					game.networkManager.putLocalDirection(Snake.Direction.LEFT);
	   				swiped=true;
	   			}
	   			
	   			if (swiped)
	   				downPoint.set(x, y);
	   			break;
	   			
	   		case MotionEvent.ACTION_DOWN:
	   			downPoint.set(x, y);
	   			break;
   			
	    }
		return true;
	}
}