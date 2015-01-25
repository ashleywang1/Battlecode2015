package classPlayer;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Supply {
	
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

	public static void runSupplyDepot() {
		
		
	}
	
	
	public static void requestSupply() throws GameActionException {
	    RobotType type = rc.getType();
        int supplyChannel, supplyLocChannel;
        if (type == RobotType.BARRACKS) {
            supplyChannel = Comms.lowestBarracksSupply;
            supplyLocChannel = Comms.lowestBarracksSupplyLoc;
        } else {
            supplyChannel = Comms.lowestMiningFactorySupply;
            supplyLocChannel = Comms.lowestMiningFactorySupplyLoc;
        }
	    MapLocation currentLoc = rc.getLocation();
	    double currentSupply = rc.getSupplyLevel();
        if (currentSupply < 100 && currentSupply < rc.readBroadcast(supplyChannel)) {
            rc.broadcast(supplyChannel, (int) currentSupply);
            rc.broadcast(supplyLocChannel, Map.locToInt(currentLoc));
            //System.out.println(currentSupply + " lowest supply");
        } else if (currentSupply > 100 && rc.readBroadcast(supplyLocChannel) == Map.locToInt(currentLoc)) {
            rc.broadcast(supplyChannel, 10000);
        }
	}
	
	public static void requestSupplyForGroup() throws GameActionException {
	    RobotType type = rc.getType();
	    int supplyChannel, supplyLocChannel;
	    if (type == RobotType.SOLDIER) {
	        supplyChannel = Comms.lowestSoldierSupply;
	        supplyLocChannel = Comms.lowestSoldierSupplyLoc;
	    } else {
	        supplyChannel = Comms.lowestMinerSupply;
	        supplyLocChannel = Comms.lowestMinerSupplyLoc;
	    }
	    MapLocation currentLoc = rc.getLocation();
	    double currentSupply = 0;
	    RobotInfo[] neighbors = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
	    for (RobotInfo n : neighbors) {
	        currentSupply += n.supplyLevel;
	    }
	    
	    double avgSupply = currentSupply / neighbors.length;
	    int strategy = rc.readBroadcast(200);
	    if (avgSupply < 30 && avgSupply < rc.readBroadcast(supplyChannel)) {
	        rc.broadcast(supplyChannel, (int) avgSupply);
	        rc.broadcast(supplyLocChannel, Map.locToInt(currentLoc));
            //System.out.println(avgSupply + " lowest avg supply");
	    } else if (strategy == 1){
	    	rc.broadcast(supplyChannel, 0);
	    	rc.broadcast(supplyLocChannel, Map.locToInt(currentLoc));
	    } else if (avgSupply > 30 && rc.readBroadcast(supplyLocChannel) == Map.locToInt(currentLoc)) {
	        rc.broadcast(supplyChannel, 10000);
	    }
	}


}
