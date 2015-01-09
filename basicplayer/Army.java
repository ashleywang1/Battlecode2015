package basicplayer;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Army {
	
	static RobotController rc = RobotPlayer.rc;
	static Team myTeam = RobotPlayer.myTeam;
	static Team enemyTeam = RobotPlayer.enemyTeam;
	
	static int myRange = RobotPlayer.myRange;
	
	static Random rand = RobotPlayer.rand;
	static Direction[] directions = RobotPlayer.directions;

	public static void runBarracks() throws GameActionException {
		RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.SOLDIER);
	}

	public static void runSoldier() throws GameActionException {
		RobotPlayer.attackEnemyZero();
		RobotPlayer.randomMove();
		
	}

	public static void runBasher() {
		// TODO Auto-generated method stub
		
	}

	public static void runTankFactory() {
		// TODO Auto-generated method stub
		
	}

	public static void runTank() {
		// TODO Auto-generated method stub
		
	}


}
