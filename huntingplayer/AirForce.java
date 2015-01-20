package huntingplayer;

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

	public static void runHelipad() throws GameActionException {
		if (rc.isCoreReady()) {
			int numDrones = rc.readBroadcast(Comms.droneCount);
			if (rc.getTeamOre() > RobotType.DRONE.oreCost) { //&& numDrones < 100
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.DRONE)) {
					
					rc.broadcast(Comms.droneCount, numDrones + 1);
				}
			}
		}
		Supply.requestSupply();
	}
	
	public static void runLauncher() {
		// TODO Auto-generated method stub
		
	}

	public static void run16Lab() {
		// TODO Auto-generated method stub
		
	}

	public static void runDrone() throws GameActionException {
	    int idChannel = Comms.memory(rc.getID());
	    if (rc.readBroadcast(idChannel) == 0) {
	        int assignment = rand.nextInt(10) + 1;
	        rc.broadcast(idChannel, assignment);
	    }
	    if (rc.readBroadcast(idChannel) < 2) {
	        becomeSupplyDrone();
	    }
	    else {
	        if (Map.inSafeArea()) {
	            Attack.hunt();	
	        } else {
	            Attack.attackTower();
	            moveAirForce();
	        }
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
			//
			
			if (outnumbered && Clock.getRoundNum() > 1800) {
				droneRush();
			} else if (helpTower != 0) {
				defendTower(helpTower);
			} else if (numDrones < 100 && strategy != 1) { 
				//rallyAround(myHQ);
				containHQ();
			} else { //RUSH
				containHQ();
				
			}
		}
		
	}
	
	public static void containHQ() throws GameActionException {
		//rallyContain(enemyHQ, RobotType.HQ.attackRadiusSquared + 100);
		MapLocation myLoc = rc.getLocation();
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		
		if (myLoc.distanceSquaredTo(enemyHQ) > RobotType.HQ.attackRadiusSquared + 10) {
			Direction toHQ = myLoc.directionTo(enemyHQ);
			if (Map.checkSafety(myLoc, toHQ)) {
				Map.tryMove(toHQ);
			} else {
				if (rand.nextDouble() < .5) {
					if (Map.checkSafety(myLoc, toHQ.opposite().rotateLeft())) {
						Map.tryMove(toHQ.opposite().rotateLeft());
					}
				} else {
					if (Map.checkSafety(myLoc, toHQ.opposite().rotateRight())) {
						Map.tryMove(toHQ.opposite().rotateRight());
					}
				}
				
			}
		}
		
		
	}

	private static void protectMiners() throws GameActionException {
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

	private static void defendTower(int help) throws GameActionException {
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
			Map.tryMove(destination);
		}
		
		Supply.requestSupplyForGroup();
			
		
	}

	private static void rallyContain(MapLocation rallyPoint, int radius) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		if (myLoc.distanceSquaredTo(rallyPoint) > radius) {
			rallyAround(rallyPoint);
		} else {
			Map.randomMove();
		}
	}
	
	
	private static void rallyAround(MapLocation rallyPoint) throws GameActionException {
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
	    if (rc.isCoreReady()) {
	        if (rc.getSupplyLevel() < 500) {
	            System.out.println("not enough supply");
	            Map.tryMove(myHQ);
	        } else {
	            if (rc.readBroadcast(Comms.lowestBarracksSupply) < 100) {
	                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestBarracksSupplyLoc));
	                Map.tryMove(lowestSupplyLoc);
	            } else if (rc.readBroadcast(Comms.lowestBasherSupply) < 30) {
	                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestBasherSupplyLoc));
	                Map.tryMove(lowestSupplyLoc);
	                //System.out.println(rc.readBroadcast(Comms.lowestSoldierSupply) + " go to soldiers");
	            } else if (rc.readBroadcast(Comms.lowestTankSupply) < 30) {
	                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestTankSupplyLoc));
	                Map.tryMove(lowestSupplyLoc);
	                System.out.println(rc.readBroadcast(Comms.lowestTankSupply) + "go to tanks");
	            } else if (rc.readBroadcast(Comms.lowestTankFactorySupply) < 100) {
	                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestTankFactorySupplyLoc));
                    Map.tryMove(lowestSupplyLoc);
	            } else if (rc.readBroadcast(Comms.lowestHelipadSupply) < 100) {
	                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestHelipadSupplyLoc));
                    Map.tryMove(lowestSupplyLoc);
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

}
