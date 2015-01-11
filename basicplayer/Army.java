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
			if (Clock.getRoundNum() > 400) {
				if (rc.getTeamOre() > RobotType.SOLDIER.oreCost) {
					RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.SOLDIER);				
				} else if (rc.getTeamOre() > RobotType.BASHER.oreCost){
					RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.BASHER);
				}
			}
		}
	      
        RobotPlayer.requestSupply();

	}

	public static void runSoldier() throws GameActionException {
		Attack.enemyZero();
		rallyRush();
		
		if (rc.getHealth() == 0) {
			int deaths = rc.readBroadcast(Comms.casualties);
			rc.broadcast(Comms.casualties, deaths + 1);
		} //I haven't used this yet TODO
		
		Ore.goProspecting();
		
		RobotPlayer.requestSupplyForGroup();
	}

	public static void runBasher() throws GameActionException {
		
		rallyRush();
	}


	public static void runTankFactory() throws GameActionException {
		if (rc.isCoreReady()) {
			if (rc.getTeamOre() > RobotType.TANK.oreCost) {
				RobotPlayer.trySpawn(directions[rand.nextInt(8)], RobotType.TANK);				
			}
		}
	}

	public static void runTank() throws GameActionException {
		Attack.enemyZero();
		rallyRush();
		
	}
	
	public static void rallyRush() throws GameActionException {
		
		int rushOver = rc.readBroadcast(Comms.rushOver);
		
		if (rc.isCoreReady()) {
			if (Clock.getRoundNum() < 1000 || rushOver == 1) {
				//Map.wanderTo(enemyHQ, .05);
				Map.randomMove();
			} else {
				MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
				if (enemyTowers.length > 0) {
					Map.tryMove(enemyTowers[0]); //if the towers are spread out
					//attack the closest one if they're all together TODO
				}else {
					Map.tryMove(enemyHQ);
				}
			}		
		}
		
	}



}
