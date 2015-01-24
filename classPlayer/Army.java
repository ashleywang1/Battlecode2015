package classPlayer;

import java.util.Random;

import battlecode.common.*;

public class Army {
	
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
	
	static boolean rush = false;

	public static void runBarracks() throws GameActionException {
		if (rc.isCoreReady()) {
			if (Clock.getRoundNum() > 200) {
				int helpTower = rc.readBroadcast(Comms.towerDistressCall);
				if (rc.getTeamOre() > RobotType.BASHER.oreCost && helpTower != 0){
					RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.BASHER);
				}
			}
		}

	}

	public static void runSoldier() throws GameActionException {
		
		Attack.attackTower();
		moveArmy();
		
	}

	public static void runBasher() throws GameActionException {
		
		if (rc.isCoreReady()) {
			int helpTower = rc.readBroadcast(Comms.towerDistressCall);
			if (helpTower != 0) {
				Map.tryMove(Map.intToLoc(helpTower));
			} else {
				Map.randomMove();
			}
			//Map.Encircle(myHQ, RobotType.HQ.attackRadiusSquared + 10); //with a wider radius TODO	
		}
	}


	public static void runTankFactory() throws GameActionException {
		if (rc.isCoreReady()) {
			int tanks = rc.readBroadcast(Comms.tanksCount);
			if (rc.getTeamOre() > RobotType.TANK.oreCost) { //&& TFnum>3 && tanks < 50
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.TANK)) {
					RobotInfo[] defenders = rc.senseNearbyRobots(myHQ, RobotType.HQ.sensorRadiusSquared*2, myTeam);
					if ( Map.nearbyRobots(defenders, RobotType.TANK) < 3) {
						rc.broadcast(Comms.TFtoSpawnedTank, 1); //make the tank a defender
					}
				}
			}
		}
	}

	public static void runTank() throws GameActionException {
		if (rc.isCoreReady()) {
			if (Map.inSafeArea(rc.getLocation())) {
				Attack.hunt(); //if no enemy in sight, moveArmy
			} else {
				if (Clock.getRoundNum() < 1600) {
					Attack.enemyZero();
					moveArmy();
				} else {
					MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
						if (enemyTowers.length > 0) {
							Map.tryMove(enemyTowers[0]); 
							Attack.attackTower();
						}else {
							Attack.enemyZero();
							if (rc.isCoreReady())
							Map.tryMove(enemyHQ);
						}
				}	
				
			}
				
		}
	}
	
	public static void moveArmy() throws GameActionException {
		
		if (rc.isCoreReady()) {
			int status = rc.readBroadcast(Comms.memory(rc.getID()));
			if (status == 1) { //defend the HQ
				tankDefender();
			} else {
				tankAttacker();
			}
		}
		
	}

	private static void tankAttacker() throws GameActionException {

		int helpTower = rc.readBroadcast(Comms.towerDistressCall);
		boolean outnumbered = (rc.senseTowerLocations().length < rc.senseEnemyTowerLocations().length + 1);
		int earlyDefense = rc.readBroadcast(Comms.defensiveRally);
		int tanks = rc.readBroadcast(Comms.tanksCount);
		
		Attack.attackTower();
		checkRushConditions(tanks);
		
		if ( Clock.getRoundNum() > 1800 && outnumbered || rush) {
			System.out.println("ground rushing");
			groundRush();
		} else if (earlyDefense != 0) {
			MapLocation firstContact = Map.intToLoc(earlyDefense);
			if (Map.inSafeArea(firstContact)) {
				AirForce.rallyAround(firstContact);	
			} else {
				containHQ();
			}
		} else {
			containHQ();
		}	
		
	}

	private static void checkRushConditions(int tanks) throws GameActionException {
		if (tanks > 35) {
			rush = true;
		}
		
		if (tanks < 20) {
			rush = false;
		}
		
	}

	private static void tankDefender() throws GameActionException {
		int radius = RobotType.HQ.attackRadiusSquared;
		if (rc.getLocation().distanceSquaredTo(myHQ) < radius) {
			Map.randomMove();
		} else {
			Map.Encircle(myHQ, radius);	
		}
		
	}

	public static void containHQ() throws GameActionException { //do what the WarMachine did
		
		MapLocation myLoc = rc.getLocation();
		int distToEnemyHQ = myLoc.distanceSquaredTo(enemyHQ);
		
		if (distToEnemyHQ > RobotType.HQ.attackRadiusSquared + 1) {
			Map.Encircle(enemyHQ, 0);
		}
	}
	
	public static void defendTower(int help) throws GameActionException {
		MapLocation toHelp = Map.intToLoc(help);
		Map.tryMove(toHelp);
		RobotInfo myTower = null;
		
		if (rc.canSenseLocation(toHelp)) {
			myTower = rc.senseRobotAtLocation(toHelp);
		}
		
		if ( myTower == null) { //tower down
			rc.broadcast(Comms.towerDistressCall, 0); //no tower to defend anymore
		}
		
	}

	private static void groundRush() throws GameActionException {

		MapLocation myLoc = rc.getLocation();
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		RobotInfo[] allies = rc.senseNearbyRobots(myRange*2,myTeam);
		
		//set destination
		MapLocation destination = enemyHQ;
		if (enemyTowers.length > 0) {
			destination = Map.myNearestTower(enemyTowers);
		}
		
		//rally around the destination then move
		if (myLoc.distanceSquaredTo(destination) > RobotType.TOWER.attackRadiusSquared + 9 || allies.length > 3) {
			Map.tryMove(destination);
		}
		
		System.out.println("ground rush toward" + destination);
		Supply.requestSupplyForGroup();
		
	}



}
