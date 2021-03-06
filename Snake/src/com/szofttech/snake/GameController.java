package com.szofttech.snake;

import java.util.Iterator;
import java.util.Random;

import android.graphics.Color;
import android.graphics.Point;

public class GameController extends Thread{
	private final Game game;
	
	private final int MAX_SKULLS=10;
	private final int MAX_STARS=10;
	
	private Snake [] snakes;
	private User [] users;
	private Snake.Direction [] snakeDirections;
	private CollectableList collectables;
	private ObjectPlacementList newObjects;
	private int [] deadSnakeDelays;
	private int [] skipSteps;
	private int fruitsNeeded;
	private boolean [] growSnakes;
	
	private static final int SNAKE_DEAD_DELAY_MS=2000;
	private static final int SNAKE_START_DELAY_MS=500;
	
	private static float KEEP_PERCENT_OF_SCORES_ON_WALL_COLLISION=0.8f;
	
	private int starCount;
	private int skullCount;
	
	private Random random;
	private Random skullRandom;
	private Random starRandom;
	
	private volatile boolean running;
	
	public void stopMe(){
		running=false;
	}
	
	public GameController(final Game game){
		this.game=game;
		running=true;
		
		random=new Random(System.currentTimeMillis());
		starRandom=new Random(System.currentTimeMillis()+Math.round(random.nextFloat()*100.0f));
		skullRandom=new Random(System.currentTimeMillis()+Math.round(random.nextFloat()*100.0f));
		
		collectables=new CollectableList();
		newObjects=new ObjectPlacementList();
		
		
		CoordinateManager.getInstance().resizeMap(game.settings.height,game.settings.width);
		
		Grid g=new Grid(game.context);
		g.setColor(Color.GRAY);
		game.renderer.addRenderable(g);
		users=game.networkManager.getUserList();
		
		snakes=new Snake[users.length];
		snakeDirections=new Snake.Direction[users.length];
		deadSnakeDelays=new int[users.length];
		skipSteps=new int[users.length];
		growSnakes=new boolean[users.length];
		fruitsNeeded=users.length;
		skullCount=0;
		starCount=0;
		for (int a=0; a<snakes.length; a++){
			snakes[a]=new Snake(game.context);
			snakes[a].setColor(users[a].color);
			snakes[a].setDead(true);
			game.renderer.addRenderable(snakes[a]);
			
			deadSnakeDelays[a]=0;
			skipSteps[a]=0;
		}
	}
	
	private int getSnakeSquaredDistances(final Point p){
		int minDistance=Integer.MAX_VALUE;
		
		for (int a=0; a<snakes.length; a++){
			minDistance=Math.min(minDistance, snakes[a].getDistanceSquared(p));
		}
		
		return minDistance;
	}
	
	private int getObjectDistances(final Point p){
		return Math.min(getSnakeSquaredDistances(p), collectables.getDistanceSquared(p));
	}
	
	private Point getRandomPlacement(int minSquredDistance){
		Point p=ObjectPool.getInstance().getPoint();
		Point mapSize=CoordinateManager.getInstance().getMapDimensions();
		
		do{
			p.x=random.nextInt(mapSize.x);
			p.y=random.nextInt(mapSize.y);
		} while (getObjectDistances(p)<=minSquredDistance);
		
		ObjectPool.getInstance().putPoint(mapSize);
				
		return p;
	}
	
	private Point getWallNormalVectorNearPoint(Point pos){
		int x=0;
		int y=0;
		
		if (pos.x==0)
			x=1;
		else if (pos.y==0)
			y=1;
		else {
			Point mapSize=CoordinateManager.getInstance().getMapDimensions();
			
			if (pos.x==(mapSize.x-1))
				x=-1;
			else if (pos.y==(mapSize.y-1))
				y=-1;
			
			ObjectPool.getInstance().putPoint(mapSize);
		}
		
		if (x!=0 || y!=0){
			Point result=ObjectPool.getInstance().getPoint();
			result.x=x;
			result.y=y;
			return result;
		}
		
		return null;		
	}
	
	private void placeDeadSnakes(){
		for (int a=0; a<snakes.length; a++){
			if (snakes[a].isDead()){
				if (deadSnakeDelays[a]!=0)
					continue;
				
				Point p=getRandomPlacement(3*3);
				
				int dx=0;
				int dy=0;
				boolean swapPoints=false;
				
			
				if (random.nextBoolean()==false){
					dx=random.nextInt(2)*2-1;
				} else {
					dy=random.nextInt(2)*2-1;
				}
			
				Point wallNormal=getWallNormalVectorNearPoint(p);
				if (wallNormal!=null){
					if (dx!=0 && wallNormal.x!=0){
						dx=wallNormal.x;
						swapPoints=true;
					} else if (dy!=0 && wallNormal.y!=0){
						dy=wallNormal.y;
						swapPoints=true;
					}
										
					ObjectPool.getInstance().putPoint(wallNormal);					
				}
				
				NewObjectPlacement p1=ObjectPool.getInstance().getNewObjectPlacement();
				NewObjectPlacement p2=ObjectPool.getInstance().getNewObjectPlacement();
				
				
				p1.position.set(p.x, p.y);
				p1.user=a;
				p1.type=NewObjectPlacement.Type.SNAKE;
				
				p2.position.set(p.x+dx, p.y+dy);
				p2.user=a;
				p2.type=NewObjectPlacement.Type.SNAKE;
				

				if (swapPoints)
					game.networkManager.putNewObjects(p2,p1);
				else
					game.networkManager.putNewObjects(p1,p2);
				
				ObjectPool.getInstance().putPoint(p);
			}
		}
	}
	
	private void placeOnRandomCoordinate(NewObjectPlacement.Type type, int minDistanceSquared){
		Point p=getRandomPlacement(minDistanceSquared);
		
		NewObjectPlacement fruit=ObjectPool.getInstance().getNewObjectPlacement();
		fruit.type=type;
		fruit.user=0;
		fruit.position.set(p.x, p.y);
		
		game.networkManager.putNewObjects(fruit);
		ObjectPool.getInstance().putPoint(p);
	}
	
	private void placeFruits(){
		for (; fruitsNeeded>0; fruitsNeeded--){
			placeOnRandomCoordinate(NewObjectPlacement.Type.FRUIT, 2*2);
		}
	}
	
	private void placeStars(){
		if (starCount<MAX_STARS){
			if (starRandom.nextFloat()<game.settings.starProbability){
				placeOnRandomCoordinate(NewObjectPlacement.Type.STAR, 2*2);
			}
		}
	}
	
	private void placeSkulls(){
		if (skullCount<MAX_SKULLS){
			if (skullRandom.nextFloat()<game.settings.starProbability){
				placeOnRandomCoordinate(NewObjectPlacement.Type.SKULL, 2*2);
			}
		}
	}
	
	private void generatePlacements(){
		if (game.isServer==false)
			return;
		
		placeDeadSnakes();
		placeFruits();
		placeStars();
		placeSkulls();
	}	
	
	
	private void mergeNewObjects(){
		game.networkManager.getNewObjects(newObjects);
		
		while (!newObjects.isEmpty()){
			NewObjectPlacement o=newObjects.pop();
			
			switch (o.type){
				case SNAKE:
					snakes[o.user].addPoint(o.position);
					snakes[o.user].setDead(false);
					skipSteps[o.user]=SNAKE_START_DELAY_MS/game.settings.stepTime;
					break;
				case FRUIT:
					Fruit f=ObjectPool.getInstance().getFruit(game.context);
					f.setPosition(o.position);
					game.renderer.addRenderable(f);
					collectables.add(f);
					break;
				case SKULL:
					Skull s=ObjectPool.getInstance().getSkull(game.context);
					s.setPosition(o.position);
					s.setTimeout(random);
					game.renderer.addRenderable(s);
					collectables.add(s);
					skullCount++;
					break;
				case STAR:
					Star star=ObjectPool.getInstance().getStar(game.context);
					star.setPosition(o.position);
					star.setTimeout(random);
					game.renderer.addRenderable(star);
					collectables.add(star);
					starCount++;
					break;
				default:
					break;
			}
			
			ObjectPool.getInstance().putNewObjectPlacement(o);
		}
	}
	
	private void dieSnake(int index){
		snakes[index].setDead(true);
		deadSnakeDelays[index]=SNAKE_DEAD_DELAY_MS/game.settings.stepTime;
	}
	
	private void moveSnakes(){
		for (int a=0; a<snakes.length; a++){
			if (!snakes[a].isSnakeValid())
				continue;
			
			if (skipSteps[a]>0){
				skipSteps[a]--;
				continue;
			}
			
			snakes[a].move(snakeDirections[a], growSnakes[a]);
		}
	}
	
	private void wallDetect(){
		for (int a=0; a<snakes.length; a++){
			if (!snakes[a].isSnakeValid())
				continue;
			
			Point pos=snakes[a].getFuturePosition(snakeDirections[a]);
			if (!CoordinateManager.getInstance().isValidPosition(pos)){
				users[a].score*=KEEP_PERCENT_OF_SCORES_ON_WALL_COLLISION;
				dieSnake(a);
			}
		}
	}
	
	private void collectableCollected(Collectable c){
		game.renderer.removeRenderable(c);
		collectables.remove(c);
		ObjectPool.getInstance().putCollectable(c);
	}
	
	
	private void collectableDetect(){
		for (int a=0; a<snakes.length; a++){
			growSnakes[a]=false;
			
			Point nextPos=snakes[a].getFuturePosition(snakeDirections[a]);
			Collectable collected=collectables.findByPos(nextPos);
			
			ObjectPool.getInstance().putPoint(nextPos);
			
			
			boolean dead=false;
			
			if (collected!=null){
				users[a].score=collected.collectedScoreTransform(users[a].score);
				growSnakes[a]=collected.isGrowNeeded();
				
				if (collected instanceof Fruit){
					fruitsNeeded++;
				} else if (collected instanceof Skull){
					dead=true;
				}
				
				collectableCollected(collected);
				
				if (dead){
					snakes[a].move(snakeDirections[a], false);
					dieSnake(a);
				}
			}
			
		}
	}
	
	private void collisionDetect(){
		wallDetect();
		collectableDetect();
	}
	
	void updateDeadSnakeDelays(){
		for (int a=0; a<snakes.length; a++){
			if (deadSnakeDelays[a]==0)
				continue;
						
			deadSnakeDelays[a]=deadSnakeDelays[a]-1;
			if (deadSnakeDelays[a]==0){
				snakes[a].clearPoints();
			}
		}
	}
	
	private void waitForNewTimeframe(){
		long endTime=game.networkManager.getFrameStartTimeInMills()+game.settings.stepTime;
		
		while (true){
			long time=System.currentTimeMillis();
			
			if (time>=endTime){
				break;
			}
			
			try {
				Thread.sleep(endTime-time);
			} catch (InterruptedException e) {
			}
		}
	}
	
	private void decreaseCollectableCount(final Collectable c){
		if (c instanceof Star)
			starCount--;
		else if (c instanceof Skull)
			skullCount--;
	}
	
	private void removeTimedOutCollectables(){
		Iterator<Collectable> i= collectables.iterator();
		while (i.hasNext()){
			Collectable c=i.next();
			if (c.isTimedOut()){
				i.remove();
				game.renderer.removeRenderable(c);
				decreaseCollectableCount(c);
				ObjectPool.getInstance().putCollectable(c);
			}
		}
	}
	
	@Override
    public void run(){
		generatePlacements();
		mergeNewObjects();
		
		while (running){
			waitForNewTimeframe();
		
			game.networkManager.getSnakeDirections(snakeDirections);
			removeTimedOutCollectables();
			updateDeadSnakeDelays();
			generatePlacements();
			mergeNewObjects();
			collisionDetect();
			moveSnakes();
		}
    }
}
