package ashleyplayer;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Tech {

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

	public static void runCommander() throws GameActionException {
		Attack.enemyZero();
		Army.moveArmy();
	}

	public static void runTrainingField() throws GameActionException {
		// TODO Auto-generated method stub
		int CommanderCount = rc.readBroadcast(Comms.commanderCount);
		if (rc.getTeamOre() > RobotType.COMMANDER.oreCost && CommanderCount == 0) {
			RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.COMMANDER);
		}
		
	}

	public static void runComputer() {
		// TODO Auto-generated method stub
		
	}

	public static void runMIT() throws GameActionException {
		// TODO Auto-generated method stub
		if (rc.getTeamOre() > 5000) {
			RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.COMPUTER);
		}
		
	}

}
