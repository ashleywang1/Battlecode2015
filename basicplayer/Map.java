package basicplayer;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class Map {

	static RobotController rc = RobotPlayer.rc;
	static Team myTeam = RobotPlayer.myTeam;
	static MapLocation myHQ = RobotPlayer.myHQ;
	static Team enemyTeam = RobotPlayer.enemyTeam;
	static MapLocation enemyHQ = RobotPlayer.enemyHQ;
	
	static int myRange = RobotPlayer.myRange;
	static Direction facing = RobotPlayer.facing;
	
	static Random rand = RobotPlayer.rand;
	static Direction[] directions = RobotPlayer.directions;


	public static int locToInt(MapLocation loc) {
		
		System.out.println(loc);
		String.format("%05d", loc.x);
		int coords = Integer.parseInt(String.format("%05d", loc.x) + String.format("%05d", loc.y));
		return coords;
	}
	
	public static MapLocation intToLoc(int i){ //problem when both coords are negative
		//System.out.println(new MapLocation((i/100000)%100000,i%100000) + "is the decoded map location");
		
		return new MapLocation((i/100000)%100000,i%100000);
	}
	
    // This method will attempt to move in Direction d (or as close to it as possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}

	public static void tryMove(MapLocation loc) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		boolean blocked = false;
		Direction d = rc.getLocation().directionTo(loc);
		int dirint = directionToInt(d);
		
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}
	
	public static void randomMove() throws GameActionException {
		if (rand.nextDouble() < .5) {
			if (rand.nextDouble() < .5) {
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
		if (rc.isCoreReady() && rc.canMove(facing)) {
			rc.move(facing);
		}
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
}
