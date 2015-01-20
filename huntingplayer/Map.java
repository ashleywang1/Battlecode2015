package huntingplayer;

import java.util.Random;


import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class Map {

	static RobotController rc = RobotPlayer.rc;
	static Team myTeam = RobotPlayer.myTeam;
	static MapLocation myHQ = RobotPlayer.myHQ;
	static Team enemyTeam = RobotPlayer.enemyTeam;
	static MapLocation enemyHQ = RobotPlayer.enemyHQ;
	static int hqID = RobotPlayer.hqID;
	
	static int myRange = RobotPlayer.myRange;
	static Direction facing = RobotPlayer.facing;
	
	static Random rand = RobotPlayer.rand;
	static Direction[] directions = RobotPlayer.directions;
	static int mapXsign = RobotPlayer.mapXsign;
	static int mapYsign = RobotPlayer.mapYsign;
	static int preference = rand.nextDouble() < .5 ? -1: 1;
	

	
    // This method will attempt to move in Direction d (or as close to it as possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}

	//This method will try to move toward a certain destination
	public static void tryMove(MapLocation loc) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		Direction d = rc.getLocation().directionTo(loc);
		int dirint = directionToInt(d);
		double prob = rand.nextDouble();
		int sign = 1;
		
		//add some randomization
		if (prob < .5) {
			sign = -1;
		}
		
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+sign*offsets[offsetIndex]+8)%8])) {
			
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+sign*offsets[offsetIndex]+8)%8]);
		}
	}
	
	//This method moves toward a destination, but never steps into a dangerous range
	public static void safeMove(MapLocation dest) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		Direction toDest = myLoc.directionTo(dest);

		if (Map.checkSafety(myLoc, toDest) && rc.canMove(toDest)) {
			rc.move(toDest);
		} else {
			if (rand.nextDouble() < .5) {
				if (Map.checkSafety(myLoc, toDest.opposite().rotateLeft())&& rc.canMove(toDest.opposite().rotateLeft())) {
					rc.move(toDest.opposite().rotateLeft());
				}
			} else {
				if (Map.checkSafety(myLoc, toDest.opposite().rotateRight()) && rc.canMove(toDest.opposite().rotateRight())) {
					rc.move(toDest.opposite().rotateRight());
				}
			}
		}
		
	}
	
	public static void Encircle(MapLocation dest, int radius) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		Direction toDest = myLoc.directionTo(dest);
		Direction[] clockwise = {toDest, toDest.rotateLeft(), toDest.rotateLeft().rotateLeft(), toDest.opposite().rotateRight(),
				toDest.opposite(), toDest.opposite().rotateLeft(), toDest.rotateRight().rotateRight(), toDest.rotateRight()};
		Direction[] counterClockwise = {toDest, toDest.rotateRight(), toDest.rotateRight().rotateRight(), 
				toDest.opposite().rotateLeft(), toDest.opposite(), toDest.opposite().rotateRight(), toDest.rotateLeft().rotateLeft(), 
				toDest.rotateLeft()};
		boolean moved = false;
		Direction[] rotation;
		rotation = (preference == 1)? clockwise : counterClockwise;
		
		if (radius == 0) { //we're attacking the enemy
			for (Direction safeDir: rotation) {
				if (Map.checkSafety(myLoc, safeDir) && rc.canMove(safeDir)) {
					rc.move(safeDir);
					moved = true;
					break;
				}
			}	
		} else {
			for (Direction safeDir: rotation) {
				if (Map.checkRadius(myLoc, safeDir, radius) && rc.canMove(safeDir)) {
					rc.move(safeDir);
					moved = true;
					break;
				}
			}
		}
		
		if (!moved || rc.senseTerrainTile(myLoc.add(rotation[2])) == TerrainTile.OFF_MAP) {
			preference = -1*preference;
		}
	}
	
	public static boolean checkSafety(MapLocation myLoc, Direction dir) {
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		boolean tileInFrontSafe = true;
		MapLocation tileInFront = myLoc.add(dir);
		for(MapLocation m: enemyTowers){
			if(m.distanceSquaredTo(tileInFront)<=RobotType.TOWER.attackRadiusSquared + 4){
				tileInFrontSafe = false;
				break;
			}
		}
		if (enemyHQ.distanceSquaredTo(tileInFront) < RobotType.HQ.attackRadiusSquared + 4) {
			tileInFrontSafe = false;
		}
		return tileInFrontSafe;
	}
	
	public static boolean checkRadius(MapLocation myLoc, Direction dir, int radius) {
		boolean goodTile = true;
		MapLocation myTile = myLoc.add(dir);
		
		if (myHQ.distanceSquaredTo(myTile) < radius) {
			goodTile = false;
		}
		return goodTile;
	}

	public static void beaverMove() throws GameActionException{
		double n = rand.nextDouble();
		if ( n < .5) {
			if (n < .25) {
				facing = facing.rotateLeft();
			} else {
				facing = facing.rotateRight();
			}
		}
		
		//avoid void and offmap tiles
	
		double p = rand.nextDouble();
		while (rc.senseTerrainTile(rc.getLocation().add(facing)) != TerrainTile.NORMAL) {
			if (p < .5) {
				facing = facing.rotateLeft();
			} else {
				facing = facing.rotateRight();
			}
		}
		//Now actually move
		//avoid going too far from HQ
		if(rc.senseNearbyRobots(myHQ, 2*RobotType.BEAVER.sensorRadiusSquared, myTeam).length >0){
			if (rc.isCoreReady() && rc.canMove(facing)) {
				rc.move(facing);
			}
		}else
			Map.tryMove(myHQ);
		
	}
	
	public static void carelessMove() throws GameActionException {
		double n = rand.nextDouble();
		if ( n < .5) {
			if (n < .25) {
				facing = facing.rotateLeft();
			} else {
				facing = facing.rotateRight();
			}
		}
		//avoid void and offmap tiles
		double p = rand.nextDouble();
		while (rc.senseTerrainTile(rc.getLocation().add(facing)) != TerrainTile.NORMAL) {
			if (p < .5) {
				facing = facing.rotateLeft();
			} else {
				facing = facing.rotateRight();
			}
		}
		//Now actually move
		if (rc.isCoreReady() && rc.canMove(facing)) {
			rc.move(facing);
		}
	}
	
	
	
	public static void randomMove() throws GameActionException {
		RobotInfo[] threats = rc.senseNearbyRobots(myRange, enemyTeam);
		int harmless = nearbyRobots(threats, RobotType.MINER);
		
		//check that the direction in front is not a tile that can be attacked by the enemy towers
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		boolean tileInFrontSafe = true;
		MapLocation tileInFront = rc.getLocation().add(facing);
		for(MapLocation m: enemyTowers){
			if(m.distanceSquaredTo(tileInFront)<=RobotType.TOWER.attackRadiusSquared + 9){
				tileInFrontSafe = false;
				break;
			}
		}
		
		if ((threats.length - harmless) > 1) {
			Direction away = threats[0].location.directionTo(myHQ);
			tryMove(away); //could go into danger
		} else if (!tileInFrontSafe) {
			
			Direction safe = findSafeTile();
			if (rc.isCoreReady() && rc.canMove(safe)) {
				rc.move(safe);
			}
		} else if(rc.senseTerrainTile(tileInFront)!=TerrainTile.NORMAL){
			double p = rand.nextDouble();
			if (p < .3) {
				facing = facing.rotateLeft();	
			} else if (p < .6) {
				facing = facing.rotateRight();
			} else {
				facing = facing.rotateLeft().rotateLeft();
			}
			
			tryMove(facing);
		} else {
			carelessMove();
		}
	}
	
    private static Direction findSafeTile() throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		for (Direction d: directions) {
			if (checkSafety(myLoc, d) && !rc.isLocationOccupied(myLoc.add(d))) {
				return d;
			}
		}
		
		return myLoc.directionTo(myHQ);
		
	}
    
	//This method should return true if robot is sufficiently far enough from enemy towers and HQ to hunt
	public static boolean inSafeArea(MapLocation loc) {
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		MapLocation myLoc = loc;
		for(MapLocation m: enemyTowers){
			if(m.distanceSquaredTo(myLoc)<=RobotType.TOWER.attackRadiusSquared + 25){
				return false;
			}
		}
		if (myLoc.distanceSquaredTo(enemyHQ) < RobotType.HQ.attackRadiusSquared + 25) {
			return false;
		}
		
		return true;
	}

	// This method will randomly move in Direction d
	static void wanderToward(Direction d, double urgency) throws GameActionException {
		double p = rand.nextDouble();
		
		if (p < urgency) {
			tryMove(d);
		} else {
			randomMove();
		}
	}

	static void wanderTo(MapLocation target, double urgency) throws GameActionException {
		double p = rand.nextDouble();
		Direction d = rc.getLocation().directionTo(target);
		
		if (p < urgency) {
			tryMove(d);
		} else {
			randomMove();
		}
	}

	public static int locToInt(MapLocation loc) {
		
		int coords = Integer.parseInt(String.format("%05d", Math.abs(loc.x)) + String.format("%05d", Math.abs(loc.y)));
		return coords;
	}
	
	public static MapLocation intToLoc(int i){ //problem when both coords are negative
		//System.out.println(new MapLocation((i/100000)%100000,i%100000) + "is the decoded map location");
		
		return new MapLocation((i/100000)%100000*mapXsign,i%100000*mapYsign);
	}

	static int directionToInt(Direction d) {
		switch(d) {
			case NORTH:
				return 0;
			case NORTH_EAST:
				return 1;
			case EAST:
				return 2;
			case SOUTH_EAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTH_WEST:
				return 5;
			case WEST:
				return 6;
			case NORTH_WEST:
				return 7;
			default:
				return -1;
		}
	}

	public static int nearbyRobots(RobotInfo[] neighbors, RobotType type) {
		int num = 0;
		for (RobotInfo x: neighbors) {
			if (x.type == type) {
				num += 1;
			}
		}
		return num;
	}
	
	public static MapLocation nearestTower(MapLocation[] enemyTowers) {
		
		MapLocation nearestTower = enemyTowers[0];
		int minDistance = enemyTowers[0].distanceSquaredTo(myHQ);
		for (MapLocation tower: enemyTowers) {
			int dist = tower.distanceSquaredTo(myHQ);
			if (dist < minDistance) {
				minDistance = dist;
				nearestTower = tower;
			}
		}
		return nearestTower;
	}

	public static void tryMoveAwayFrom(MapLocation enemyLocation) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		Direction d = rc.getLocation().directionTo(enemyLocation);
		int dirint = directionToInt(d) + 4; //want to move away from
		double prob = rand.nextDouble();
		int sign = 1;
		
		//add some randomization
		if (prob < .5) {
			sign = -1;
		}
		
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+sign*offsets[offsetIndex]+8)%8])) {
			
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+sign*offsets[offsetIndex]+8)%8]);
		}		
	}


}
