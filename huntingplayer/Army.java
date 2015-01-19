package huntingplayer;

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

	public static void runBarracks() throws GameActionException {
		if (rc.isCoreReady()) {
			if (Clock.getRoundNum() > 200) {
				int helpTower = rc.readBroadcast(Comms.towerDistressCall);
				if (rc.getTeamOre() > RobotType.BASHER.oreCost && helpTower != 0){
					if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.BASHER)) {
						int numBashers = rc.readBroadcast(Comms.basherCount);
						rc.broadcast(Comms.basherCount, numBashers + 1);
					}
				//int numSupplyDepots = rc.readBroadcast(Comms.supplydepotCount);
				//int numTankFactories = rc.readBroadcast(Comms.tankfactoryCount);
				}
				/*else if (rc.getTeamOre() > RobotType.SOLDIER.oreCost) {
					if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.SOLDIER)) {
						int numSoldiers = rc.readBroadcast(Comms.soldierCount);
						rc.broadcast(Comms.soldierCount, numSoldiers + 1);
					}
				} */
			}
		}
	      
        //Supply.requestSupply();

	}

	public static void runSoldier() throws GameActionException {
		RobotInfo [] enemies = Attack.getEnemiesInAttackingRange(RobotType.SOLDIER);
		Attack.attackTower();
		
		moveArmy();
		//they're kinda useless
		
		
	}

	public static void runBasher() throws GameActionException {
		int helpTower = rc.readBroadcast(Comms.towerDistressCall);
		AirForce.defendTower(helpTower);
		//moveArmy();
	}


	public static void runTankFactory() throws GameActionException {
		int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
		if (rc.isCoreReady()) {
			int tanks = rc.readBroadcast(Comms.tanksCount);
			if (rc.getTeamOre() > RobotType.TANK.oreCost) { //&& TFnum>3 && tanks < 50
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.TANK)) {
					rc.broadcast(Comms.tanksCount, tanks + 1);
				}
			}
		}
	}

	public static void runTank() throws GameActionException {
		if (Map.inSafeArea()) {
			Attack.hunt(); //if no enemy in sight, moveArmy
		} else {
			Attack.attackTower();
			moveArmy();
		}
		
	}
	
	public static void moveArmy() throws GameActionException {
		
		int strategy = rc.readBroadcast(200);
		MapLocation myLoc = rc.getLocation();
		int rushOver = rc.readBroadcast(Comms.rushOver);
		
		//System.out.println("soldiers get that strategy is : " + strategy + " and rushOver is : " + rushOver);
		
		if (rc.isCoreReady()) {
			
			int helpTower = rc.readBroadcast(Comms.towerDistressCall);
			boolean outnumbered = (rc.senseTowerLocations().length < rc.senseEnemyTowerLocations().length + 1);
			int earlyDefense = rc.readBroadcast(Comms.defensiveRally);
			
			if (outnumbered && Clock.getRoundNum() > 1800) {
				groundRush();
			} else if (helpTower != 0 && rc.getType() == RobotType.SOLDIER) {
				defendTower(helpTower);
			} else if (earlyDefense != 0 && Clock.getRoundNum() < 500) {
				AirForce.rallyAround(Map.intToLoc(earlyDefense));
			} else {
				AirForce.containHQ();
			}
			
		}
		
	}

	private static void shutDown(int enemyDetected) throws GameActionException {
		MapLocation breach = Map.intToLoc(enemyDetected);
		if (breach.distanceSquaredTo(myHQ) > breach.distanceSquaredTo(enemyHQ)){ //attack
			Map.tryMove(breach);
		} else {
			Map.tryMove(myHQ);
		}
		//Map.tryMove(toHelp);
	}

	public static void defendTower(int help) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		RobotInfo[] allies = rc.senseNearbyRobots(myRange,myTeam);
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
		RobotInfo[] allies = rc.senseNearbyRobots(myRange,myTeam);
		int rushStart = rc.readBroadcast(Comms.rushStartRound);
		
		//set destination
		MapLocation destination = enemyHQ;
		if (enemyTowers.length > 0) {
			//destination = enemyTowers[0];
			destination = Map.nearestTower(enemyTowers);
			//attack the closest one if they're all together TODO
		}
		
		//rally around the destination then move
		if (myLoc.distanceSquaredTo(destination) > RobotType.TOWER.attackRadiusSquared + 9 ||
				allies.length > 3) {
			Map.tryMove(destination);
		}
		
		
		if (Clock.getRoundNum() > rushStart + 200) {
			checkRushOver();	
		}
		Supply.requestSupplyForGroup();
		
	}

	private static void checkRushOver() throws GameActionException {
		int rush = rc.readBroadcast(Comms.rushOver);
		if (rush == 0) { //in the middle of a rush
			if (rc.senseNearbyRobots(myRange, myTeam).length < 1) { // no clumping
				rc.broadcast(Comms.rushOver, 1);
				rc.broadcast(Comms.rushStartRound, 0);
				rc.broadcast(Comms.tanksCount, 0);
				rc.broadcast(Comms.basherCount, 0);
			}
		}
		
	}



}
