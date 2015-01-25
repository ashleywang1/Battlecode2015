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
	static int rotation = -1;

	public static void runHelipad() throws GameActionException {
		if (rc.isCoreReady()) {
			int numDrones = rc.readBroadcast(Comms.droneCount);
			if (rc.getTeamOre() > RobotType.DRONE.oreCost && numDrones < 10) { //&& numDrones < 100
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.DRONE)) {
				    
					rc.broadcast(Comms.droneCount, numDrones + 1);
			if (rc.getTeamOre() > RobotType.DRONE.oreCost + RobotType.TANK.oreCost && numDrones < 10) {
				RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.DRONE);
			}
		}
		
	}
	
	public static void runLauncher() throws GameActionException {
			int currentRound = Clock.getRoundNum();
			double n = rand.nextDouble();

			MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
			MapLocation[] myTowers = rc.senseTowerLocations();
			RobotInfo [] nearbyFriends = rc.senseNearbyRobots(25, myTeam);
			int numLaunch = Map.nearbyRobots(nearbyFriends, RobotType.LAUNCHER);
			
			Attack.launchNearbyMissiles();
			if(numLaunch<5){
				if(myTowers.length>0){
					MapLocation nearestMyTower = Map.nearestTower(myTowers);
					if(rc.isCoreReady())
						Map.safeMove(nearestMyTower);
				}
			}else{
				if(rc.isCoreReady() && enemyTowers.length>0)
					Map.safeMove(enemyTowers[0]);
				else if (rc.isCoreReady())
					Map.safeMove(enemyHQ);
			}

			

			
//			if(currentRound>1600){
//				if(rc.isCoreReady())
//					Army.groundRush();
//				Attack.launchMissiles();
//			}else{
//				if(rc.isCoreReady()){
//					Army.moveArmy();
//				Attack.launchNearbyMissiles();
//				}
//			}
				
		
	}
	
	public static void runMissile() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(25, enemyTeam);
		if(rc.isCoreReady()){

			if (enemies.length > 0 && rc.canMove(rc.getLocation().directionTo(enemies[0].location))) {
				
				//System.out.println("Missile headed toward tower from " + rc.getLocation());
				rc.setIndicatorString(0,"trying to move "+Clock.getRoundNum() +  rc.canMove(rc.getLocation().directionTo(enemies[0].location)));
				rc.move(rc.getLocation().directionTo(enemies[0].location));
			}
		}


		
	}

	public static void run16Lab() throws GameActionException {
		
		if(rc.isCoreReady()){
			int numLaunchers = rc.readBroadcast(Comms.launcherCount);
			if (rc.getTeamOre() > RobotType.LAUNCHER.oreCost) {
			if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.LAUNCHER)) {
				
				rc.broadcast(Comms.launcherCount, numLaunchers + 1);
			}
		}
		}
		
	}

	public static void runDrone() throws GameActionException {
//		if (Map.inSafeArea()) {
//			Attack.hunt();	
//		} else {
//			Attack.attackTower();
//			moveAirForce();
//		}
	    
	    int idChannel = Comms.memory(rc.getID());
	    int assignment = rc.readBroadcast(idChannel);
	    if (assignment == 0)
	        rc.broadcast(idChannel, rand.nextInt(10) + 1);
		RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, enemyTeam);
		if (enemies.length > 1)
		    if (rc.isCoreReady())
		        Map.tryMoveAwayFrom(enemies[0].location);
		    else;
		else if (enemies.length > 0)
		    Attack.enemyZero();
		else
		    becomeSupplyDrone();
	}
	

	public static void moveAirForce() throws GameActionException {
		
		if (rc.isCoreReady()) {
			
			int helpTower = rc.readBroadcast(Comms.towerDistressCall);
			boolean outnumbered = (rc.senseTowerLocations().length < rc.senseEnemyTowerLocations().length + 1);
			int numDrones = rc.readBroadcast(Comms.droneCount);
			//
			//int droneTarget = rc.readBroadcast(Comms.droneTarget);
			int droneDefense = rc.readBroadcast(Comms.droneRallyPoint);
			
			if (outnumbered && Clock.getRoundNum() > 1800) {
				droneRush();
			} else if (helpTower != 0) {
				defendTower(helpTower);
			} else if (numDrones < 100 && strategy != 1) { 
				//rallyAround(myHQ);
				containHQ();
			} else if (droneDefense != 0 && numDrones < 20) {
				int radius = RobotType.HQ.attackRadiusSquared + 25;
				if (rc.getLocation().distanceSquaredTo(myHQ) < radius) {
					Map.randomMove();
				} else {
					Map.Encircle(myHQ, radius);	
				}
			} else {
				protectMiners();
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
	     
	public static void protectMiners() throws GameActionException {
		int oreLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
		int minerEnemies = rc.readBroadcast(Comms.enemiesNearMiners);
		
		//supply them too
		if (rc.getSupplyLevel() < 200) {
			Map.safeMove(myHQ);
		} else {
			if (oreLoc != 0 && minerEnemies == 0) {
				int radius = 25;
				MapLocation dest = Map.intToLoc(oreLoc);
				if (rc.getLocation().distanceSquaredTo(dest) < radius) {
					Map.randomMove();
				} else {
					Map.safeMove(dest);
				}
			} else if (minerEnemies != 0) {
				rallyAround(Map.intToLoc(minerEnemies));
			} else {
				Map.randomMove();
			}	
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
		if(Clock.getRoundNum()>1800){
			Map.tryMove(enemyHQ);
		}
		else if (myLoc.distanceSquaredTo(destination) > RobotType.TOWER.attackRadiusSquared  ||
				allies.length > 3) {
			Map.tryMove(destination);
		}
		
		Supply.requestSupplyForGroup();
			
		
	}

	private static void rallyContain(MapLocation rallyPoint, int radius) throws GameActionException {
		if (myLoc.distanceSquaredTo(rallyPoint) > radius) {
			rallyAround(rallyPoint);
		} else {
			Map.randomMove();
		}
		
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
	    int assignment = rc.readBroadcast(Comms.memory(rc.getID()));
	    
	    rc.setIndicatorString(1, assignment + "");
	    if (rc.isCoreReady()) {
	        if (rc.getSupplyLevel() < 250) {
	            Map.tryMove(myHQ);
	            rc.setIndicatorString(0, "go to hq");
	            rc.setIndicatorString(2, "need more supply");
	        } else {
	            if (assignment < 3) {
	                if (checkMinerSupply())
	                    return;
	                else if (checkMiningFactorySupply())
	                    return;
	            }
	            else if (assignment < 8) {
	                if (checkTankSupply())
	                    return;
	                else if (checkTankFactorySupply())
	                    return;
	            }
	            if (checkTankSupply());
	            else if (checkTankFactorySupply());
	            else if (checkBasherSupply());
	            else if (checkBarracksSupply());
	            else if (checkMinerSupply());
	            else if (checkMiningFactorySupply());
	            else if (checkHelipadSupply());
	            else {
	                //Map.randomMove();
                    Map.tryMove(myHQ);
                    rc.setIndicatorString(0, "go to hq");
                    rc.setIndicatorString(2, "nothing to do");
	            }
	        }
	    }
	}
	
	private static boolean checkTankSupply() throws GameActionException {
	    if (rc.readBroadcast(Comms.lowestTankSupply) < 30) {
            MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestTankSupplyLoc));
            Map.tryMove(lowestSupplyLoc);
            rc.setIndicatorString(0, "go to tanks");
            rc.setIndicatorString(2, lowestSupplyLoc.toString());
            //System.out.println(rc.readBroadcast(Comms.lowestTankSupply) + "go to tanks");
            return true;
	    }
	    return false;
	}
	
	private static boolean checkTankFactorySupply() throws GameActionException {
	    if (rc.readBroadcast(Comms.lowestTankFactorySupply) < 100) {
            MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestTankFactorySupplyLoc));
            Map.tryMove(lowestSupplyLoc);
            rc.setIndicatorString(0, "go to tank factory");
            rc.setIndicatorString(2, lowestSupplyLoc.toString());
            return true;
	    }
	    return false;
	}
	
	private static boolean checkBasherSupply() throws GameActionException {
	    if (rc.readBroadcast(Comms.lowestBasherSupply) < 30) {
            MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestBasherSupplyLoc));
            Map.tryMove(lowestSupplyLoc);
            rc.setIndicatorString(0, "go to bashers");
            rc.setIndicatorString(2, lowestSupplyLoc.toString());
            //System.out.println(rc.readBroadcast(Comms.lowestSoldierSupply) + " go to soldiers");
            return true;
        }
	    return false;
	}
	
	private static boolean checkBarracksSupply() throws GameActionException {
	    /*if (rc.readBroadcast(Comms.lowestBarracksSupply) < 100) {
            MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestBarracksSupplyLoc));
            Map.tryMove(lowestSupplyLoc);
            rc.setIndicatorString(0, "go to barracks");
            rc.setIndicatorString(2, lowestSupplyLoc.toString());
            return true;
        }*/
	    return false;
	}
	
	private static boolean checkMinerSupply() throws GameActionException {
	    if (rc.readBroadcast(Comms.lowestMinerSupply) < 30) {
            MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestMinerSupplyLoc));
            Map.tryMove(lowestSupplyLoc);
            rc.setIndicatorString(0, "go to miners");
            rc.setIndicatorString(2, lowestSupplyLoc.toString());
            //System.out.println(rc.readBroadcast(Comms.lowestMinerSupply) + " go to miners");
            return true;
	    }
	    return false;
	}
	
	private static boolean checkMiningFactorySupply() throws GameActionException {
	    if (rc.readBroadcast(Comms.lowestMiningFactorySupply) < 100) {
            MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestMiningFactorySupplyLoc));
            Map.tryMove(lowestSupplyLoc);
            rc.setIndicatorString(0, "go to mining factory");
            rc.setIndicatorString(2, lowestSupplyLoc.toString());
            return true;
        }
	    return false;
	}
	
	private static boolean checkHelipadSupply() throws GameActionException {
	    if (rc.readBroadcast(Comms.lowestHelipadSupply) < 100) {
            MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestHelipadSupplyLoc));
            Map.tryMove(lowestSupplyLoc);
            rc.setIndicatorString(0, "go to helipad");
            rc.setIndicatorString(2, lowestSupplyLoc.toString());
            return true;
        }
	    return false;
	}

}
