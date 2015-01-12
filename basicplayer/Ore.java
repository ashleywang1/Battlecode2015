package basicplayer;

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
		if (rc.isCoreReady() && numMiners < 80 && 
				((rc.getTeamOre() >= RobotType.MINER.oreCost && round < 400) || 
						(rc.getTeamOre() >= RobotType.MINER.oreCost + 200))) {
			int oreFields = rc.readBroadcast(Comms.bestOreFieldLoc);
			
			if (oreFields == 0) {
				success = RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.MINER);
			} else {
				MapLocation oreLoc = Map.intToLoc(oreFields);
				success = RobotPlayer.trySpawn(rc.getLocation().directionTo(oreLoc), RobotType.MINER);
			}
			
			//broadcast and update numMiners
			if (success) {
				rc.broadcast(Comms.minerCount, numMiners + 1);			
			}
		     
	        RobotPlayer.requestSupply();
		}
		
		//if under attack, broadcast for help
		if (rc.getHealth() < 20) {
			int numMF = rc.readBroadcast(Comms.miningfactoryCount);
			rc.broadcast(Comms.miningfactoryCount, numMF - 1);
		}

	}


	public static void runMiner() throws GameActionException {
		
		MapLocation spawnPoint;
		
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		int minerEnemies = Map.nearbyRobots(enemies, RobotType.MINER);
		RobotInfo[] allies = rc.senseNearbyRobots(myRange-2, myTeam);
		//avoid enemies, tolerate allies to 3
		//walls = rc.senseTerrainTile(rc.getLocation());
		
		if (rc.isCoreReady()) {
			MapLocation myLoc = rc.getLocation();
			MapLocation miner = nearestMiner(allies);
			if (miner!=null) { //if you're not crowded allies.length < 3 &&
				if ((rc.senseOre(myLoc) < 12)) {
					minerMove(myLoc);	
				} else {
					Direction away = miner.directionTo(myLoc);
					Map.tryMove(away); //move away from others
					/*double p = rand.nextDouble();
					if (p < .9) {
											
					}*/
					/*else if (p < .8) {
						Map.tryMove(away.rotateLeft().rotateLeft());
					} else if (p < .9) {
						Map.tryMove(away.rotateRight().rotateRight());
					} else {
						rc.mine();
					}*/
				}				
			} else if ((enemies.length - minerEnemies) > 0) {
				Map.tryMove(enemies[0].location.directionTo(myLoc)); //move away from others
			} else if (rc.senseOre(myLoc) > 12) {
				if (rand.nextDouble() < .8) {
					rc.mine();	
				} else {
					Map.tryMove(facing);
				}
				
			} else {
				minerMove(myLoc);
			}
		}
		
		RobotPlayer.requestSupplyForGroup();
		
	}
	
	private static void minerMove(MapLocation myLoc) throws GameActionException {
		
		int oreLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
		double front = rc.senseOre(myLoc.add(facing));
		double right = rc.senseOre(myLoc.add(facing.rotateRight()));
		double left = rc.senseOre(myLoc.add(facing.rotateLeft()));
		if (oreLoc != 0) {
			MapLocation oreField = Map.intToLoc(oreLoc);
			//System.out.println(oreField + "is where I'm going");
			Map.tryMove(myLoc.directionTo(oreField));			
		}
		else {
			Map.randomMove();
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
		//TODO eventually don't just judge one square, make it all 16 squares in range
		//eventually once this region dries up, change the best ore field
		
		int maxOre = Math.max(rc.readBroadcast(Comms.bestOreFieldAmount), 20);
		int oreDistance = rc.readBroadcast(Comms.bestOreFieldDistance);
		MapLocation myLoc = (rc.getLocation());
		int myDistance = myLoc.distanceSquaredTo(myHQ);
		double ore = surroundingOre(myLoc);
		int loc = rc.readBroadcast(Comms.bestOreFieldLoc);
		MapLocation mapCoords = Map.intToLoc(loc);
		
		if (ore > maxOre*9 && myDistance > oreDistance) {
			
			int coords = Map.locToInt(rc.getLocation());
			System.out.println("HEY FOUND BETTER" + coords); //TODO
			rc.broadcast(Comms.bestOreFieldLoc, coords);
			rc.broadcast(Comms.bestOreFieldAmount, (int) (ore/9.0) );
		} else if (myLoc.equals(mapCoords) && rc.senseOre(myLoc) < maxOre*9) {
			rc.broadcast(Comms.bestOreFieldAmount, 0);
			rc.broadcast(Comms.bestOreFieldLoc, 0);
			System.out.println("find another minefield");
		}
		
	}
}
