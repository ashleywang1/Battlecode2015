package huntingplayer;

import java.util.Random;

import huntingplayer.Comms;
import huntingplayer.RobotPlayer;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class AirForce {

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
	static int rotation = -1;

	public static void runHelipad() throws GameActionException {
		if (rc.isCoreReady()) {
			int numDrones = rc.readBroadcast(Comms.droneCount);
			int numairspace = rc.readBroadcast(Comms.aerospacelabCount);
			if (rc.getTeamOre() > RobotType.DRONE.oreCost && numDrones < 10) {
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.DRONE)) {
					
					rc.broadcast(Comms.droneCount, numDrones + 1);
				}
			}
			if (rc.getTeamOre() > RobotType.AEROSPACELAB.oreCost && numairspace<3) {
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.AEROSPACELAB)) {
					rc.broadcast(Comms.aerospacelabCount, numairspace + 1);
				}
			}
		}
		
	}
	
	public static void runLauncher() throws GameActionException {
		Attack.launchMissiles();
		droneRush();
//		if (Map.inSafeArea()) {
//			droneRush();//if no enemy in sight, moveArmy
//		} else {
//			Attack.attackTower();
//			Army.moveArmy();
//		}
		
	}
	public static void runMissile() throws GameActionException {
		if(rc.isCoreReady()){
			rc.move(rc.getLocation().directionTo(rc.senseEnemyTowerLocations()[0]));
		}
		
	}

	public static void run16Lab() throws GameActionException {
		int numLaunchers = rc.readBroadcast(Comms.launcherCount);
		if(rc.isCoreReady()){
			if (rc.getTeamOre() > RobotType.LAUNCHER.oreCost) {
			if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.LAUNCHER)) {
				
				rc.broadcast(Comms.launcherCount, numLaunchers + 1);
			}
		}
		}
		
	}

	public static void runDrone() throws GameActionException {
		if (Map.inSafeArea()) {
			Attack.hunt();	
		} else {
			Attack.attackTower();
			droneRush();
		}
		
	}
	
	public static void moveAirForce() throws GameActionException {
		
		int strategy = rc.readBroadcast(200);
		MapLocation myLoc = rc.getLocation();
		int rushOver = rc.readBroadcast(Comms.rushOver);
		//System.out.println("soldiers get that strategy is : " + strategy + " and rushOver is : " + rushOver);
		
		if (rc.isCoreReady()) {
			
			int helpTower = rc.readBroadcast(Comms.towerDistressCall);
			boolean outnumbered = (rc.senseTowerLocations().length < rc.senseEnemyTowerLocations().length + 1);
			int numDrones = rc.readBroadcast(Comms.droneCount);
			int droneTarget = rc.readBroadcast(Comms.droneTarget);
			int droneDefense = rc.readBroadcast(Comms.droneRallyPoint);
			
			if (outnumbered && Clock.getRoundNum() > 1800) {
				droneRush();
			} else if (helpTower != 0) {
				defendTower(helpTower);
			} else if (droneDefense != 0 && numDrones < 20) {
				rallyAround(myHQ);
			} else if (numDrones < 5 && strategy != 1) { 
				//
				//containHQ();
				protectMiners();
			} else { //RUSH
				containHQ();
			}
		}
		
	}
	
	public static void containHQ() throws GameActionException { //do what the WarMachine did
		
		MapLocation myLoc = rc.getLocation();
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		int distToEnemyHQ = myLoc.distanceSquaredTo(enemyHQ);
		//Map.safeMove(enemyHQ);
		//rallyContain(enemyHQ, RobotType.HQ.attackRadiusSquared + 4);
		
		if (distToEnemyHQ > RobotType.HQ.attackRadiusSquared + 5) {
			Map.safeMove(enemyHQ);
		}
	}

	public static void protectMiners() throws GameActionException {
		int oreLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
		int minerEnemies = rc.readBroadcast(Comms.enemiesNearMiners);
		int numDrones = rc.readBroadcast(Comms.droneCount);
		
		if (oreLoc != 0 && minerEnemies == 0) {
			//rallyAround(Map.intToLoc(oreLoc));
			rallyContain(Map.intToLoc(oreLoc), numDrones);
		} else if (minerEnemies != 0) {
			rallyAround(Map.intToLoc(minerEnemies));
		} else {
			Map.randomMove();
		}
	}

	public static void defendTower(int help) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		RobotInfo[] allies = rc.senseNearbyRobots(myRange,myTeam);
		MapLocation toHelp = Map.intToLoc(help);
		Map.tryMove(toHelp);
	}

	private static void droneRush() throws GameActionException {

		MapLocation myLoc = rc.getLocation();
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		RobotInfo[] allies = rc.senseNearbyRobots(myRange,myTeam);
		int rushStart = rc.readBroadcast(Comms.rushStartRound);
		int rushOver = rc.readBroadcast(Comms.rushOver);
		int strategy = rc.readBroadcast(Comms.strategy);
		
		MapLocation destination = enemyHQ;
		if (enemyTowers.length > 0) {
			destination = Map.nearestTower(enemyTowers);
			//attack the least defended one TODO
		}
		
		//rally around the destination then move
		if (myLoc.distanceSquaredTo(destination) > RobotType.TOWER.attackRadiusSquared + 25 ||
				allies.length > 3) {
			if(rc.isCoreReady())
			Map.tryMove(destination);
		}
		
		Supply.requestSupplyForGroup();
			
		
	}

	private static void rallyContain(MapLocation rallyPoint, int radius) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		
/*		
		if (myLoc.distanceSquaredTo(rallyPoint) > radius) {
			rallyAround(rallyPoint);
		} else {
			//Map.randomMove();
			Direction angle = rallyPoint.directionTo(myLoc);
			if (rotation == -1) {
				angle = angle.rotateRight();
			} else {
				angle = angle.rotateLeft();
			}
			Map.tryMove(rallyPoint.add(angle.rotateLeft(), radius));
			if (rc.senseTerrainTile(myLoc.add(angle)) == TerrainTile.OFF_MAP) {
				rotation = -1*rotation;
			}
		}*/
	}
	
	
	public static void rallyAround(MapLocation rallyPoint) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		Direction toRallyPoint = rc.getLocation().directionTo(rallyPoint);
		int dirint = Map.directionToInt(toRallyPoint);
		boolean blocked = false;
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}
	
	private static void becomeSupplyDrone() throws GameActionException { //tania please make this work T.T TODO
		if (rc.getSupplyLevel() < 500) {
            Map.tryMove(myHQ);
        } else {
             if (rc.readBroadcast(Comms.lowestBarracksSupply) < 100) {
                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestBarracksSupplyLoc));
                Map.tryMove(lowestSupplyLoc);
            } else if (rc.readBroadcast(Comms.lowestSoldierSupply) < 30) {
                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestSoldierSupplyLoc));
                Map.tryMove(lowestSupplyLoc);
                //System.out.println(rc.readBroadcast(Comms.lowestSoldierSupply) + " go to soldiers");
            } else if (rc.readBroadcast(Comms.lowestMinerSupply) < 30) {
                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestMinerSupplyLoc));
                Map.tryMove(lowestSupplyLoc);
                //System.out.println(rc.readBroadcast(Comms.lowestMinerSupply) + " go to miners");
            } else if (rc.readBroadcast(Comms.lowestMiningFactorySupply) < 100) {
                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestMiningFactorySupplyLoc));
                Map.tryMove(lowestSupplyLoc);
            } else {
            	Map.randomMove();
            }
        }
	}


}
