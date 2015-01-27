package huntingplayer;

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

	public static void runSupplyDepot() throws GameActionException {
		Supply.reportSupply();
		
	}
	
	
	public static void requestSupply() throws GameActionException {
	    RobotType type = rc.getType();
        int supplyChannel, supplyLocChannel;
        if (type == RobotType.BARRACKS) {
            supplyChannel = Comms.lowestBarracksSupply;
            supplyLocChannel = Comms.lowestBarracksSupplyLoc;
        } else if (type == RobotType.MINERFACTORY) {
            supplyChannel = Comms.lowestMiningFactorySupply;
            supplyLocChannel = Comms.lowestMiningFactorySupplyLoc;
        } else if (type == RobotType.HELIPAD) {
            supplyChannel = Comms.lowestHelipadSupply;
            supplyLocChannel = Comms.lowestHelipadSupplyLoc;
        } else if (type == RobotType.AEROSPACELAB) {
            supplyChannel = Comms.lowestAerospaceLabSupply;
            supplyLocChannel = Comms.lowestAerospaceLabSupplyLoc;
        } else {
            supplyChannel = Comms.lowestTankFactorySupply;
            supplyLocChannel = Comms.lowestTankFactorySupplyLoc;
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
	    if (type == RobotType.BASHER) {
	        supplyChannel = Comms.lowestBasherSupply;
	        supplyLocChannel = Comms.lowestBasherSupplyLoc;
	    } else if (type == RobotType.MINER) {
	        supplyChannel = Comms.lowestMinerSupply;
	        supplyLocChannel = Comms.lowestMinerSupplyLoc;
	    } else if (type == RobotType.LAUNCHER) {
	        supplyChannel = Comms.lowestLauncherSupply;
	        supplyLocChannel = Comms.lowestLauncherSupplyLoc;
	    } else {
	        supplyChannel = Comms.lowestTankSupply;
	        supplyLocChannel = Comms.lowestTankSupplyLoc;
	    }
	    MapLocation currentLoc = rc.getLocation();
	    double currentSupply = 0;
	    RobotInfo[] neighbors = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
	    for (RobotInfo n : neighbors) {
	        currentSupply += n.supplyLevel;
	    }
	    
	    double avgSupply = currentSupply / neighbors.length;
	    int strategy = rc.readBroadcast(200);
	    if (avgSupply < 30 && avgSupply <= rc.readBroadcast(supplyChannel)) {
	        rc.broadcast(supplyChannel, (int) avgSupply);
	        rc.broadcast(supplyLocChannel, Map.locToInt(currentLoc));
            System.out.println(avgSupply + " lowest avg supply");
	    } else if (strategy == 1){
	    	rc.broadcast(supplyChannel, 0);
	    	rc.broadcast(supplyLocChannel, Map.locToInt(currentLoc));
	    } else if (avgSupply > 30 && rc.readBroadcast(supplyLocChannel) - Map.locToInt(currentLoc) < 10) {
	        rc.broadcast(supplyChannel, 10000);
	    }
	}
	
	public static void reportSupply() throws GameActionException {
	    int maxSupply = rc.readBroadcast(Comms.highestSupply);
	    int avgSupply = (int) rc.getSupplyLevel();
	    RobotInfo[] neighbors = rc.senseNearbyRobots(rc.getLocation(), 8, myTeam);
	    for (RobotInfo n : neighbors) {
	        avgSupply += n.supplyLevel;
	    }
	    avgSupply /= (neighbors.length + 1);
	    if (avgSupply > maxSupply) {
	        rc.broadcast(Comms.highestSupply, (int) rc.getSupplyLevel());
	        rc.broadcast(Comms.highestSupplyLoc, Map.locToInt(rc.getLocation()));
	    } else if (avgSupply < 400 && rc.readBroadcast(Comms.highestSupplyLoc) - Map.locToInt(rc.getLocation()) < 8) {
	        rc.broadcast(Comms.highestSupply, avgSupply); 
	    }
	}


}
