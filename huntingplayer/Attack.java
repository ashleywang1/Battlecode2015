package huntingplayer;

import java.util.Random;

import battlecode.common.*;

public class Attack {

	
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

	static int enemyToHunt = -1;
	static MapLocation enemyLocation = null;
	static RobotType enemyType = null;

	public static void hunt() throws GameActionException {
		if (rc.isCoreReady()) {
			if (enemyToHunt == -1) {
				RobotInfo[] enemies = rc.senseNearbyRobots(myRange * 2, enemyTeam);
				if (enemies.length > 0) {
					chooseTarget(enemies);
				}
			}
			if (enemyToHunt != -1) {
				//update location
				enemyLocation = findEnemyRobotNear(enemyLocation, enemyToHunt);
				if (enemyLocation == null) {
					// either robot died or he went out of range
					enemyToHunt = -1;
					enemyLocation = null;
					enemyType = null;
				} else {
					// if we can attack, do so
					if(rc.getType()==RobotType.LAUNCHER){
						launchNearbyMissiles();
					}
					else if (rc.canAttackLocation(enemyLocation) && rc.isWeaponReady()) {
						rc.attackLocation(enemyLocation);
					} else {
						// if we have a larger range than the enemy, try to stay out of their range
						if (enemyType.attackRadiusSquared < myRange) {
							int distance = rc.getLocation().distanceSquaredTo(enemyLocation);
							if (distance > myRange) {
								Map.tryMove(enemyLocation);
							} else if (distance < enemyType.attackRadiusSquared) {
								Map.tryMoveAwayFrom(enemyLocation);
							}
						}
						// else just move towards them
						else {
							if (rc.getLocation().distanceSquaredTo(enemyLocation) >2)
								Map.tryMove(enemyLocation);
						}
					}
				}
			}
			else { //didn't detect any enemies
				RobotType type = rc.getType();
				if (type == RobotType.TANK) {
					Army.moveArmy();	
				} else if(type !=RobotType.LAUNCHER){
					AirForce.moveAirForce();
				}
				
			}
		}
	}

	public static void launchNearbyMissiles() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(30, enemyTeam);
		RobotInfo[] myRobots = rc.senseNearbyRobots(25, myTeam);
		MapLocation[] towers = rc.senseEnemyTowerLocations();
		
		boolean missileExist = false;
		for(RobotInfo b: myRobots){
			if(b.type == RobotType.MISSILE)
				missileExist = true;
		}
		if (enemies.length > 0  && rc.canLaunch(rc.getLocation().directionTo(enemies[0].location))) {
			rc.launchMissile(rc.getLocation().directionTo(enemies[0].location));
		}
		
		if(towers.length>0 ){
			MapLocation nearestTower = Map.nearestTower(towers);
			if(rc.getLocation().distanceSquaredTo(nearestTower) <=25){
				if(rc.canLaunch(rc.getLocation().directionTo(nearestTower))){
				
					rc.launchMissile(rc.getLocation().directionTo(nearestTower));
			}
			}
		}
		
	}

	private static void chooseTarget(RobotInfo[] enemies) {
		
		for (RobotInfo enemy: enemies) {
			
			if (enemy.type == RobotType.HQ || enemy.type == RobotType.TOWER) {
				continue;
			} else if (enemy.type != RobotType.MINER) { //prioritize armies over miners
				enemyToHunt = enemy.ID;
				enemyLocation = enemy.location;
				enemyType = enemy.type;
				break;
			} else {
				enemyToHunt = enemy.ID;
				enemyLocation = enemy.location;
				enemyType = enemy.type;
			}
		}
	}
	
	public static void launchMissiles() throws GameActionException {
		MapLocation[] towers = rc.senseEnemyTowerLocations();
		
			if(towers.length>0 && Clock.getRoundNum()>1800){
				
				if(rc.canLaunch(rc.getLocation().directionTo(towers[0]))){
					
				rc.launchMissile(rc.getLocation().directionTo(towers[0]));
				}
			}
			else if(Clock.getRoundNum()>1800){
				if(rc.canLaunch(rc.getLocation().directionTo(rc.senseEnemyHQLocation()))){
					rc.launchMissile(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
				}
			}else{
				
				RobotInfo[] enemies = rc.senseNearbyRobots(10, enemyTeam);
				if (enemies.length > 0 && rc.canLaunch(rc.getLocation().directionTo(enemies[0].location))) {
					rc.launchMissile(rc.getLocation().directionTo(enemies[0].location));
				}
				
			}
	}

	public static MapLocation findEnemyRobotNear(MapLocation location, int id) {
		RobotInfo[] info = rc.senseNearbyRobots(location, 4, enemyTeam);
		for (RobotInfo robot : info) {
			if (robot.ID == id) {
				return robot.location;
			}
		}
		return null;
	}

	public static RobotInfo[] getEnemiesInAttackingRange(RobotType type) {
		RobotInfo[] enemies = rc.senseNearbyRobots(type.attackRadiusSquared,
				enemyTeam);
		return enemies;
	}

	public static void lowestHP(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length == 0) {
			return;
		}

		if (rc.isWeaponReady()) {
			double minHP = Double.MAX_VALUE;
			MapLocation toAttack = null;
			for (RobotInfo info : enemies) {
				if (info.health < minHP) {
					toAttack = info.location;
					minHP = info.health;
				}
			}

			rc.attackLocation(toAttack);
		}
	}

	public static void enemyZero() throws GameActionException {
		if (rc.isWeaponReady()) {
			RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
			if (enemies.length > 0) {
				rc.attackLocation(enemies[0].location);
			}
		}
	}

	public static void attackTower() throws GameActionException {
		MapLocation nearbyTower = null;
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),
				rc.getType().attackRadiusSquared, rc.getTeam().opponent());

		if (nearbyEnemies.length > 0 && rc.isWeaponReady()) { // there exists
																// enemies near
			// find the first tower and shoot at it
			for (RobotInfo info : nearbyEnemies) {
				if (info.type.equals(RobotType.TOWER)) {
					nearbyTower = info.location;
					break;
				}
			}
			if (nearbyTower != null && rc.canAttackLocation(nearbyTower)) {
				rc.attackLocation(nearbyTower);
			} else {
				lowestHP(nearbyEnemies);
			}
		}
	}

}
