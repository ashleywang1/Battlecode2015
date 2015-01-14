package basicplayer;

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
//				if (rc.getTeamOre() > RobotType.BASHER.oreCost && rand.nextDouble() < .4){
//					if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.BASHER)) {
//						int numBashers = rc.readBroadcast(Comms.basherCount);
//						rc.broadcast(Comms.basherCount, numBashers + 1);
//					}
				int numSupplyDepots = rc.readBroadcast(Comms.supplydepotCount);
				int numTankFactories = rc.readBroadcast(Comms.tankfactoryCount);
				} else if (rc.getTeamOre() > RobotType.SOLDIER.oreCost) {
					if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.SOLDIER)) {
						int numSoldiers = rc.readBroadcast(Comms.soldierCount);
						rc.broadcast(Comms.soldierCount, numSoldiers + 1);
					}
				} 
			}
		
	      
        Supply.requestSupply();

	}

	public static void runSoldier() throws GameActionException {
		RobotInfo [] enemies = Attack.getEnemiesInAttackingRange(RobotType.SOLDIER);
		Attack.attackTower();
		moveArmy();
		
		if (rc.getHealth() < 1) {
			int deaths = rc.readBroadcast(Comms.casualties);
			rc.broadcast(Comms.casualties, deaths + 1);
		} //I haven't used this yet TODO
		
		Ore.goProspecting();
		
	}

	public static void runBasher() throws GameActionException {
		
		moveArmy();
	}


	public static void runTankFactory() throws GameActionException {
		int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
		if (rc.isCoreReady()) {
			int tanks = rc.readBroadcast(Comms.tanksCount);
			if (rc.getTeamOre() > RobotType.TANK.oreCost && TFnum>3 && tanks < 50) {
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.TANK)) {
					rc.broadcast(Comms.tanksCount, tanks + 1);
				}
			}
		}
	}

	public static void runTank() throws GameActionException {
		Ore.goProspecting();
		Attack.enemyZero();
		moveArmy();
		
	}
	
	public static void moveArmy() throws GameActionException {
		
		int strategy = rc.readBroadcast(200);
		MapLocation myLoc = rc.getLocation();
		int rushOver = rc.readBroadcast(Comms.rushOver);
		//System.out.println("soldiers get that strategy is : " + strategy + " and rushOver is : " + rushOver);
		
		if (rc.isCoreReady()) {
			
			int helpTower = rc.readBroadcast(Comms.towerDistressCall);
			boolean outnumbered = (rc.senseTowerLocations().length < rc.senseEnemyTowerLocations().length + 1);
			
			if (outnumbered && Clock.getRoundNum() > 1800) {
				groundRush();
			} else if (helpTower != 0 && rc.getType() == RobotType.SOLDIER) {
				defendTower(helpTower);
			} else if (strategy == 1) { //RUSH!
				groundRush();
			} else {
				//shut down TODO
				Map.randomMove();
			}
		}
		
	}

	public static void defendTower(int help) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		RobotInfo[] allies = rc.senseNearbyRobots(myRange,myTeam);
		MapLocation toHelp = Map.intToLoc(help);
		Map.tryMove(toHelp);
		RobotInfo myTower = rc.senseRobotAtLocation(toHelp);
		
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
