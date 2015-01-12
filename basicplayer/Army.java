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
				if (rc.getTeamOre() > RobotType.SOLDIER.oreCost) {
					if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.SOLDIER)) {
						int numSoldiers = rc.readBroadcast(Comms.soldierCount);
						rc.broadcast(Comms.soldierCount, numSoldiers + 1);
					}
				} else if (rc.getTeamOre() > RobotType.BASHER.oreCost){
					if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.BASHER)) {
						int numBashers = rc.readBroadcast(Comms.basherCount);
						rc.broadcast(Comms.basherCount, numBashers + 1);
					}
				}
			}
		}
	      
        Supply.requestSupply();

	}

	public static void runSoldier() throws GameActionException {
		RobotInfo [] enemies = Attack.getEnemiesInAttackingRange(RobotType.SOLDIER);
		Attack.attackTower();
		rallyRush();
		
		if (rc.getHealth() < 1) {
			int deaths = rc.readBroadcast(Comms.casualties);
			rc.broadcast(Comms.casualties, deaths + 1);
		} //I haven't used this yet TODO
		
		Ore.goProspecting();
		
	}

	public static void runBasher() throws GameActionException {
		
		rallyRush();
	}


	public static void runTankFactory() throws GameActionException {
		if (rc.isCoreReady()) {
			int tanks = rc.readBroadcast(Comms.tanksCount);
			if (rc.getTeamOre() > RobotType.TANK.oreCost && tanks < 50) {
				if (RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.TANK)) {
					rc.broadcast(Comms.tanksCount, tanks + 1);
				}
			}
		}
	}

	public static void runTank() throws GameActionException {
		Attack.enemyZero();
		rallyRush();
		
	}
	
	public static void rallyRush() throws GameActionException {
		
		int strategy = rc.readBroadcast(200);

		int rushOver = rc.readBroadcast(Comms.rushOver);
		//System.out.println("soldiers get that strategy is : " + strategy + " and rushOver is : " + rushOver);
		
		if (rc.isCoreReady()) {
			if (Clock.getRoundNum() < 500 || rushOver == 1) {
				//Map.wanderTo(enemyHQ, .05);
				Map.randomMove();
			} else {
				MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
				if(strategy==1){
					int rushStart = rc.readBroadcast(Comms.rushStartRound);
					//set destination
					MapLocation destination = enemyHQ;
					if (enemyTowers.length > 0) {
						destination = enemyTowers[0];
						//attack the closest one if they're all together TODO
					}
					
					//rally around the destination then move
					if (rc.getLocation().distanceSquaredTo(destination) > RobotType.TOWER.attackRadiusSquared + 9 ||
							rc.senseNearbyRobots(myRange,myTeam).length > 3) {
						Map.tryMove(destination);	
					}
					
					
					if (Clock.getRoundNum() > rushStart + 200) {
						checkRushOver();	
					}
					Supply.requestSupplyForGroup();
				}else{
					Map.randomMove();
					//TODO set rally around towers? center?
				}
			}		
		}
		
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
