package classPlayer;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Ore {
	
	static RobotController rc = RobotPlayer.rc;
	static Team myTeam = RobotPlayer.myTeam;
	static MapLocation myHQ = RobotPlayer.myHQ;
	static MapLocation[] myTowers = RobotPlayer.myTowers;
	static Team enemyTeam = RobotPlayer.enemyTeam;
	static MapLocation enemyHQ = RobotPlayer.enemyHQ;
	static MapLocation[] enemyTowers = RobotPlayer.enemyTowers;
	
	static int myRange = RobotPlayer.myRange;
	static Direction facing = RobotPlayer.facing;
	
	static Random rand = RobotPlayer.rand;
	static Direction[] directions = RobotPlayer.directions;
	
	
	public static void runMinerFactory() throws GameActionException {
		
		boolean success = false;
		int numMiners = rc.readBroadcast(Comms.minerCount);
		int round = Clock.getRoundNum();
		if (rc.isCoreReady() && numMiners < 100 && 
				((rc.getTeamOre() >= RobotType.MINER.oreCost && round < 400) || 
						(rc.getTeamOre() >= RobotType.MINER.oreCost + RobotType.TANK.oreCost))) {
			int oreFields = rc.readBroadcast(Comms.bestOreFieldLoc);
			
			if (oreFields == 0) {
				success = RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.MINER);
			} else {
				MapLocation oreLoc = Map.intToLoc(oreFields);
				success = RobotPlayer.trySpawn(rc.getLocation().directionTo(oreLoc), RobotType.MINER);
			}
			
		     
	        Supply.requestSupply();
		}
		
		//if under attack, broadcast for help
		if (rc.getHealth() < 20) {
			int numMF = rc.readBroadcast(Comms.miningfactoryCount);
			rc.broadcast(Comms.miningfactoryCount, numMF - 1);
		}

	}


	public static void runMiner() throws GameActionException {
		
		if (rc.isCoreReady()) {
			RobotInfo[] enemies = rc.senseNearbyRobots(myRange*3, enemyTeam);
			RobotInfo[] allies = rc.senseNearbyRobots(myRange-2, myTeam);
			
			MapLocation myLoc = rc.getLocation();
			MapLocation miner = nearestMiner(allies);
			if (enemies.length > 0) {
				Map.tryMoveAwayFrom(enemies[0].location);
			} else if (rc.senseOre(myLoc) > 2) {
				if (miner != null && rand.nextDouble() < .3) {
					if (!Map.checkSafety(myLoc, facing)) {
						facing = Map.findSafeTile();
					}
					Map.tryMove(facing);
				} else {
					rc.mine();	
				}
				
			} else {
				minerMove(myLoc);
			}
		}
		
		Supply.requestSupplyForGroup();
		
	}
	
	public static void minerMove(MapLocation myLoc) throws GameActionException {
		
		int oreLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
		
		if (oreLoc != 0) {
			MapLocation oreField = Map.intToLoc(oreLoc);
			Direction toOre = myLoc.directionTo(oreField);
			
			if (Map.checkSafety(myLoc, toOre)) {
				if (Map.checkSafety(myLoc, toOre)) {
					Map.tryMove(toOre);
				}
			} else {
				localOre(myLoc);
			}
		} else {
			localOre(myLoc);
		}
		
	}


	private static void localOre(MapLocation myLoc) throws GameActionException {
		Direction back = myLoc.directionTo(myHQ);
		
		double maxOre = rc.senseOre(myLoc.add(facing));
		
		for (Direction dir: directions) {
			double dirOre = rc.senseOre(myLoc.add(dir));
			if (dirOre > maxOre) {
				maxOre = dirOre;
				facing = dir;
			}
		}
		
		if (Map.checkSafety(myLoc, facing)) {
			Map.tryMove(facing);
		} else {
			if (rand.nextDouble() < .5) {
				if (Map.checkSafety(myLoc, facing.opposite().rotateLeft())) {
					Map.tryMove(facing.opposite().rotateLeft());
				}
			} else {
				if (Map.checkSafety(myLoc, facing.opposite().rotateRight())) {
					Map.tryMove(facing.opposite().rotateRight());
				}
			}	
		}
		
	}


	private static MapLocation nearestMiner(RobotInfo[] allies) {
		for (RobotInfo ri: allies) {
			if (ri.type == RobotType.MINER) {
				return ri.location;
			}
		}
		return null;
	}

	public static double surroundingOre(MapLocation myLoc) {
		
		double ore = rc.senseOre(myLoc);
		for (Direction dir: directions) {
			ore += rc.senseOre(myLoc.add(dir));
		}
		
		return ore;
	}

	public static void goProspecting() throws GameActionException {

		int maxOre = Math.max(rc.readBroadcast(Comms.bestOreFieldAmount), 100);
		int oreDistance = rc.readBroadcast(Comms.bestOreFieldDistance);
		int loc = rc.readBroadcast(Comms.bestOreFieldLoc);
		
		MapLocation myLoc = rc.getLocation();
		int myDistance = myLoc.distanceSquaredTo(enemyHQ);
		double ore = surroundingOre(myLoc);
		MapLocation mapCoords = Map.intToLoc(loc);
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange*2, enemyTeam);
		int harmless = Map.nearbyRobots(enemies, RobotType.MINER);
		
		boolean safe = Map.checkSafety(mapCoords.add(facing.opposite()), facing);
		
		if ((int) ore >= maxOre && myDistance > oreDistance) {
			int coords = Map.locToInt(rc.getLocation());
			rc.broadcast(Comms.bestOreFieldLoc, coords);
			rc.broadcast(Comms.bestOreFieldAmount, (int) ore );
		} else if (myLoc.equals(mapCoords) && (!safe || ore < 10 || enemies.length - harmless > 0)) {
			rc.broadcast(Comms.bestOreFieldAmount, 0);
			rc.broadcast(Comms.bestOreFieldLoc, 0);
			//System.out.println("find another minefield");
		}
		
	}
}
