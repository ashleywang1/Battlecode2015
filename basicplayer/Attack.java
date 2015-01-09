package basicplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class Attack {
	
	static RobotController rc = RobotPlayer.rc;
	static Team myTeam = RobotPlayer.myTeam;
	static Team enemyTeam = RobotPlayer.enemyTeam;
	
	static int myRange = RobotPlayer.myRange;
	
	static Random rand = RobotPlayer.rand;
	static Direction[] directions = RobotPlayer.directions;
	

	public static void nearbyEnemy() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}
	
	public static void attackEnemyZero() throws GameActionException {
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		//shoot at any enemy (can choose which type later, lowest hp etc)
		if(nearbyEnemies.length>0){ //there exists enemies near
			//try to shoot at enemy specified by nearbyenemies[0]
			if(rc.isWeaponReady()&&rc.canAttackLocation(nearbyEnemies[0].location)){
				rc.attackLocation(nearbyEnemies[0].location);
			}
		}
	}
	
	public static void attackLowHP() throws GameActionException {
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(myRange);
		List<Double> enemyHP = new ArrayList<Double>();
		for(RobotInfo x: nearbyEnemies){
			double hp = x.health;
			enemyHP.add(hp);
		}
	}
}
