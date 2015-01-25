package ashleyplayer;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

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
			if (rc.getTeamOre() > RobotType.DRONE.oreCost + RobotType.TANK.oreCost) { //&& numDrones < 100
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.DRONE)) {
					
					rc.broadcast(Comms.droneCount, numDrones + 1);
				}
			}
		}
		
	}
	
	public static void runLauncher() {
		// TODO Auto-generated method stub
		
	}

	public static void run16Lab() {
		// TODO Auto-generated method stub
		
	}

	public static void runDrone() throws GameActionException {
		rc.senseNearbyRobots(myRange*2, myTeam);
		
		if (Map.inSafeArea(rc.getLocation())) {
			Attack.hunt();	
		} else {
			Attack.attackTower();
			moveAirForce();
		}
		
	}
	
	public static void moveAirForce() throws GameActionException {
		
		//int strategy = rc.readBroadcast(200);
		//MapLocation myLoc = rc.getLocation();
		//int rushOver = rc.readBroadcast(Comms.rushOver);
		
		if (rc.isCoreReady()) {
			
			int helpTower = rc.readBroadcast(Comms.towerDistressCall);
			boolean outnumbered = (rc.senseTowerLocations().length < rc.senseEnemyTowerLocations().length + 1);
			int numDrones = rc.readBroadcast(Comms.droneCount);
			//int droneTarget = rc.readBroadcast(Comms.droneTarget);
			int droneDefense = rc.readBroadcast(Comms.droneRallyPoint);
			
			if (outnumbered && Clock.getRoundNum() > 1800) {
				droneRush();
			} else if (helpTower != 0) {
				defendTower(helpTower);
			} else if (droneDefense != 0 && numDrones < 20) {
				rallyAround(myHQ);
			} else if (numDrones < 5) { 
				protectMiners();
			} else {
				protectMiners();
				//TODO
			}
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
		MapLocation toHelp = Map.intToLoc(help);
		Map.tryMove(toHelp);
	}

	private static void droneRush() throws GameActionException {

		MapLocation myLoc = rc.getLocation();
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		RobotInfo[] allies = rc.senseNearbyRobots(myRange,myTeam);
		
		MapLocation destination = enemyHQ;
		if (enemyTowers.length > 0) {
			destination = Map.nearestTower(enemyTowers);
			//attack the least defended one TODO
		}
		
		//rally around the destination then move
		if (myLoc.distanceSquaredTo(destination) > RobotType.TOWER.attackRadiusSquared + 25 ||
				allies.length > 3) {
			Map.tryMove(destination);
		}
		
		Supply.requestSupplyForGroup();
			
		
	}

	private static void rallyContain(MapLocation rallyPoint, int radius) throws GameActionException {
		
		//TODO
	}
	
	
	public static void rallyAround(MapLocation rallyPoint) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		Direction toRallyPoint = rc.getLocation().directionTo(rallyPoint);
		int dirint = Map.directionToInt(toRallyPoint);
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
