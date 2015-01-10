package basicplayer;

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
	

	public static void enemyZero() throws GameActionException {
		if (rc.isWeaponReady()) {
			RobotPlayer.attackEnemyZero();
		}
	}
	
	
	
	
	//if you sense a tower nearby, stop and attack only the tower TODO

}
