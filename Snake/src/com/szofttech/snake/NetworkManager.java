package com.szofttech.snake;

public interface NetworkManager {
	public abstract void getSnakeDirections(Snake.Direction [] destionation);
	public abstract void putLocalDirection(Snake.Direction direction);
	
	public abstract long getFrameStartTimeInMills();
	
	public abstract void putNewObjects(NewObjectPlacement... object);
	public abstract void getNewObjects(ObjectPlacementList objects);
	
	public abstract User [] getUserList();
}
