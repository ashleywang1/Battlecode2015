package basicplayer;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Army {
	
	static RobotController rc = RobotPlayer.rc;
	static Team myTeam = RobotPlayer.myTeam;
	static MapLocation myHQ = RobotPlayer.myHQ;
	static Team enemyTeam = RobotPlayer.enemyTeam;
	static MapLocation enemyHQ = RobotPlayer.enemyHQ;
	
	static int myRange = RobotPlayer.myRange;
	static Direction facing = RobotPlayer.facing;
	
	static Random rand = RobotPlayer.rand;
	static Direction[] directions = RobotPlayer.directions;

	public static void runBarracks() throws GameActionException {
		if (rc.isCoreReady()) {
			if (rc.getTeamOre() > RobotType.SOLDIER.oreCost) {
				RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.SOLDIER);				
			}
		}
	}

	public static void runSoldier() throws GameActionException {
		if (rc.isWeaponReady()) {
			RobotPlayer.attackEnemyZero();
		}
		if (rc.isCoreReady()) {
			if (Clock.getRoundNum() < 1200) {
				//Map.wanderTo(enemyHQ, .05);
				Map.randomMove();
			} else {
				Map.tryMove(enemyHQ);
				//RobotPlayer.goProspecting();
			}
				
		}
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
