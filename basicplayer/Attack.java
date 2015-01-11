package basicplayer;

import java.util.Random;

import battlecode.common.*;


public class Attack {


	static RobotController rc = RobotPlayer.rc;
	static Team myTeam = RobotPlayer.myTeam;
	static Team enemyTeam = RobotPlayer.enemyTeam;
	
	static int myRange = RobotPlayer.myRange;
	
	static Random rand = RobotPlayer.rand;
	static Direction[] directions = RobotPlayer.directions;
	

	public static void enemyZero() throws GameActionException {
		if (rc.isWeaponReady()) {
			RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
			if (enemies.length > 0) {
				rc.attackLocation(enemies[0].location);
			}
		}
	}
	
    public static RobotInfo[] getEnemiesInAttackingRange(RobotType type) {
        RobotInfo[] enemies = rc.senseNearbyRobots(type.attackRadiusSquared, enemyTeam);
        return enemies;
    }
	
    public static void lowestHP(RobotInfo[] enemies) throws GameActionException {
        if (enemies.length == 0) {
            return;
        }

        if (rc.isWeaponReady()){
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
	

        
    public static void attackTower() throws GameActionException{
    	MapLocation nearbyTower = null;
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		if(nearbyEnemies.length>0){ //there exists enemies near
			//find the first tower and shoot at it
			for (RobotInfo info : nearbyEnemies) {
        		if (info.type.equals(RobotType.TOWER)) {
        			nearbyTower = info.location;
        			break;
        		}
        	}
			if(nearbyTower!=null&&rc.isWeaponReady()&&rc.canAttackLocation(nearbyTower)){
				rc.attackLocation(nearbyTower);
			}
		}
    }
}
