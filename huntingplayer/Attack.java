package huntingplayer;

import java.util.Random;

import battlecode.common.*;

public class Attack {

	static RobotController rc = RobotPlayer.rc;
	static Team myTeam = RobotPlayer.myTeam;
	static Team enemyTeam = RobotPlayer.enemyTeam;

	static int myRange = RobotPlayer.myRange;

	static Random rand = RobotPlayer.rand;
	static Direction[] directions = RobotPlayer.directions;

	static int enemyToHunt = -1;
	static MapLocation enemyLocation = null;
	static RobotType enemyType = null;

	public static void enemyZero() throws GameActionException {
		if (rc.isWeaponReady()) {
			RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
			if (enemies.length > 0) {
				rc.attackLocation(enemies[0].location);
			}
		}
	}

	public static void hunt() throws GameActionException {
		if (rc.isCoreReady()) {
			if (enemyToHunt == -1) {
				RobotInfo[] enemies = rc.senseNearbyRobots(myRange * 2, enemyTeam);
				if (enemies.length > 0) {
					RobotInfo enemy = enemies[rand.nextInt(enemies.length)];
					enemyToHunt = enemy.ID;
					enemyLocation = enemy.location;
					enemyType = enemy.type;
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
					if (rc.canAttackLocation(enemyLocation) && rc.isWeaponReady()) {
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
			else {
				Map.wanderTo(RobotPlayer.enemyHQ, 0.6);
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