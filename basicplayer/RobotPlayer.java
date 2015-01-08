package basicplayer;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
	static RobotController rc;
	static Team myTeam;
	static MapLocation myHQ;
	static Team enemyTeam;
	static MapLocation enemyHQ;
	
	static int myRange;
	static Direction facing;
	
	static Random rand;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	
	public static void run(RobotController RC) {
		rc = RC;
        rand = new Random(rc.getID());

		myRange = rc.getType().attackRadiusSquared;
		facing = Direction.values()[(int)(rand.nextDouble()*8)];
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
        Direction lastDirection = null;
		myTeam = rc.getTeam();
		myHQ = rc.senseHQLocation();
		enemyTeam = myTeam.opponent();
		enemyHQ = rc.senseEnemyHQLocation();
		
				
		while(true) {
			try {
				//information
                rc.setIndicatorString(0, "This is an indicator string.");
                rc.setIndicatorString(1, "I am a " + rc.getType());
                
                //MAIN
        		if (rc.getType() == RobotType.HQ) {
        			runHQ();
        		} else if (rc.getType() == RobotType.BEAVER) {
        			runBeaver();
        		}
        		//MINING units
        		else if (rc.getType() == RobotType.MINERFACTORY) {
        			runMinerFactory();
        		} else if (rc.getType() == RobotType.MINER) {
        			runMiner();
        		}
        		/*
        		// * ARMY
        		//GROUND ARMY units
        		else if (rc.getType() == RobotType.BARRACKS) {
        			Army.runBarracks();
        		} else if (rc.getType() == RobotType.SOLDIER) {
        			Army.runSoldier();
        		} else if (rc.getType() == RobotType.BASHER) {
        			runBasher();
        		} else if (rc.getType() == RobotType.TANKFACTORY) {
        			runTankFactory();
        		} else if (rc.getType() == RobotType.TANK) {
        			runTank();
        		}
        	
        		//SUPPLY
        		else if (rc.getType() == RobotType.SUPPLYDEPOT) {
        			runSupplyDepot();
        		}
        		//TECHNOLOGY ARMY units
        		else if (rc.getType() == RobotType.TECHNOLOGYINSTITUTE) {
        			runMIT();
        		} else if (rc.getType() == RobotType.COMPUTER) {
        			runComputer();
        		} else if (rc.getType() == RobotType.TRAININGFIELD) {
        			runTrainingField();
        		} else if (rc.getType() == RobotType.COMMANDER) {
        			runCommander();
        		}
        		//AIR ARMY units
        		else if (rc.getType() == RobotType.HELIPAD) {
        			runHelipad();
        		} else if (rc.getType() == RobotType.DRONE) {
        			runDrone();
        		} else if (rc.getType() == RobotType.AEROSPACELAB) {
        			run16Lab();
        		} else if (rc.getType() == RobotType.LAUNCHER) {
        			runLauncher();
        		}
        		*/
        		transferSupplies();
        		
        		rc.yield();
        		
            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();

            }

		}
	}


	private static void runHQ() throws GameActionException {

		RobotInfo[] myRobots;
		
		//int fate = rand.nextInt(10000);
		//myRobots = rc.senseNearbyRobots(999999, myTeam);
		
		boolean spawnSuccess = false;
		
		if (rc.isWeaponReady()) {
			attackEnemyZero();
		}
		if (rc.isCoreReady()) { //&& fate < Math.pow(1.2,12-numBeavers)*10000
			int numBeavers = rc.readBroadcast(Comms.beaverCount);
			if (rc.getTeamOre() >= 100 && numBeavers < 5) 
			spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
			if (spawnSuccess) {
				if (rand.nextDouble() < .6) {
					rc.broadcast(Comms.HQtoSpawnedBeaver, 1); //make it a barrack				
				}
				rc.broadcast(Comms.beaverCount, numBeavers + 1);
			}
		}
		
	}


	private static void runBeaver() throws GameActionException {
		if (rc.isWeaponReady()) {
			attackEnemyZero();
		}
		if (rc.isCoreReady()) {
			int round = Clock.getRoundNum(); //Is there a more efficient place to put this? TODO
			
			
			if ( round < 500) {
				//
				int numMiningFactories = rc.readBroadcast(Comms.miningfactoryCount);
				int numBarracks = rc.readBroadcast(Comms.barracksCount);
				System.out.println(numMiningFactories + "MF count and Barracks are " + numBarracks);
				//beaver 1 = mf
				//beaver 2-4 = barracks
				//beaver 5 = mf
				int assignment = rc.readBroadcast(rc.getID()%10000);
				if (assignment == 0) { //minerfactory
					if (rc.getTeamOre() >= 500 && numMiningFactories < 2) {
						System.out.println("team ore over 500, ok ok");
						if (rc.senseOre(rc.getLocation()) >= 20 ) {
							System.out.println("sensed over 20 at location ok ok");
							boolean success = tryBuild(directions[rand.nextInt(8)],RobotType.MINERFACTORY);
						}
					} else if (rand.nextDouble() < .5 && rc.senseOre(rc.getLocation())>=20){
						rc.mine();
					} else {

						int oreFieldLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
						System.out.println(oreFieldLoc + "is where the best minefield is");
						if (oreFieldLoc != 0) {
							System.out.println(intToLoc(oreFieldLoc) + "is where we're supposed to go");
							randomMove();
						} else {
							Direction awayFromHQ = myHQ.directionTo(rc.getLocation());
							tryMove(awayFromHQ);
						}

					}
				}
				if (assignment == 1) { //barracks
					RobotInfo[] neighbors = rc.senseNearbyRobots(4);
					ArrayList<MapLocation> nearbyTowers = new ArrayList<>();
					ArrayList<MapLocation> nearbyBarracks = new ArrayList<>();
					for (RobotInfo x: neighbors) {
						if (x.type == RobotType.TOWER) {
							nearbyTowers.add(x.location);
						}
						if (x.type == RobotType.BARRACKS) {
							nearbyBarracks.add(x.location); //right now not using locations, later could just make them ints
						}
					}
					if (nearbyTowers.size() > 0 && nearbyBarracks.size() == 0) {
						Direction toEnemy = rc.getLocation().directionTo(enemyHQ);
						tryBuild(toEnemy,RobotType.BARRACKS);
					} else {
						tryMove(rc.senseTowerLocations()[rand.nextInt(5)]);					
					}	
				}
				
				goProspecting();
			}
			else {
				int fate = rand.nextInt(1000);
				if (fate < 600) {
					rc.mine();
				} else if (fate < 800) {
					tryMove(directions[rand.nextInt(8)]);
				} else {
					tryMove(rc.senseHQLocation().directionTo(rc.getLocation()));
				}
			}
		}
	}

	private static void runMinerFactory() throws GameActionException {
		
		boolean success = false;
		int numMiners = rc.readBroadcast(Comms.minerCount);
		if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.MINER.oreCost && numMiners < 50) {
			trySpawn(directions[rand.nextInt(8)], RobotType.MINER);
			success = true;
		}
		//broadcast and update numMiners
		if (success) {
			rc.broadcast(Comms.minerCount, numMiners + 1);			
		}
		
	}


	private static void runMiner() throws GameActionException {
		
		MapLocation spawnPoint;
		RobotInfo[] neighbors;
		
		neighbors = rc.senseNearbyRobots(5);
		if (rc.isCoreReady()) {
			if (neighbors.length > 0) {
				tryMove(neighbors[0].location.directionTo(rc.getLocation())); //move away from others
			} else if (rc.senseOre(rc.getLocation()) > 12) {
				rc.mine();
			} else {
				int loc = rc.readBroadcast(Comms.bestOreFieldLoc);
				tryMove(intToLoc(loc));
			}
		}
		
	}
	
	private static void goProspecting() throws GameActionException {
		//check (the center of?) all corners of your sight
		double ore = rc.senseOre(rc.getLocation());
		int loc = rc.readBroadcast(Comms.bestOreFieldLoc);
		if (ore >= 30 && loc == 0) {
			
			int coords = locToInt(rc.getLocation());
			System.out.println("HEY FOUND IT" + coords); //TODO
			rc.broadcast(Comms.bestOreFieldLoc, coords);
		}
	}
	
	private static int locToInt(MapLocation loc) {
		System.out.println(loc.y + "why is this weird?");
		System.out.println(loc);
		String.format("%04d", loc.x);
		int coords = Integer.parseInt(Integer.toString(loc.x) + Integer.toString(loc.y));
		return coords;
	}
	
	public static MapLocation intToLoc(int i){
		return new MapLocation((i/10000)%10000,i%10000);
	}


	static void attackEnemyZero() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}

    // This method will attempt to build in the given direction (or as close to it as possible)
	static boolean tryBuild(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		boolean success = false;
		while (offsetIndex < 8 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.build(directions[(dirint+offsets[offsetIndex]+8)%8], type);
			success = true;
		}
		return success;
	}
	
    // This method will attempt to move in Direction d (or as close to it as possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}

	private static void tryMove(MapLocation loc) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		boolean blocked = false;
		Direction d = rc.getLocation().directionTo(loc);
		int dirint = directionToInt(d);
		
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}
	
	private static void randomMove() throws GameActionException {
		if (rand.nextDouble() < .5) {
			if (rand.nextDouble() < .5) {
				facing = facing.rotateLeft();
			} else {
				facing = facing.rotateRight();
			}
		}
		//avoid void and offmap tiles
		double p = rand.nextDouble();
		while (rc.senseTerrainTile(rc.getLocation().add(facing)) != TerrainTile.NORMAL) {
			if (p < .5) {
				facing = facing.rotateLeft();
			} else {
				facing = facing.rotateRight();
			}
		}
		if (rc.isCoreReady() && rc.canMove(facing)) {
			rc.move(facing);
		}
	}
	
    // This method will randomly move in Direction d
	static void wanderToward(Direction d, double urgency) throws GameActionException {
		double p = rand.nextDouble();
		
		if (p < urgency) {
			tryMove(d);
		} else {
			randomMove();
		}
	}

	static void wanderTo(MapLocation target, double urgency) throws GameActionException {
		double p = rand.nextDouble();
		Direction d = rc.getLocation().directionTo(target);
		
		if (p < urgency) {
			tryMove(d);
		} else {
			randomMove();
		}
	}
	
	// This method will attempt to spawn in the given direction (or as close to it as possible)
	static boolean trySpawn(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		boolean success = false;
		while (offsetIndex < 8 && !rc.canSpawn(directions[(dirint+offsets[offsetIndex]+8)%8], type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint+offsets[offsetIndex]+8)%8], type);
			success = true;
		}
		return success; //use this to determine if spawn was successful or not
	}
	
	private static void transferSupplies() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
        double lowestSupply = rc.getSupplyLevel();
        double transferAmount = 0;
        MapLocation suppliesToThisLocation = null;
        for(RobotInfo ri:nearbyAllies){
            if(ri.supplyLevel<lowestSupply){
                lowestSupply = ri.supplyLevel;
                transferAmount = (rc.getSupplyLevel()-ri.supplyLevel)/2;
                suppliesToThisLocation = ri.location;
            }
        }
        if(suppliesToThisLocation!=null){
            rc.transferSupplies((int)transferAmount, suppliesToThisLocation);
        }
    }
	
	static int directionToInt(Direction d) {
		switch(d) {
			case NORTH:
				return 0;
			case NORTH_EAST:
				return 1;
			case EAST:
				return 2;
			case SOUTH_EAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTH_WEST:
				return 5;
			case WEST:
				return 6;
			case NORTH_WEST:
				return 7;
			default:
				return -1;
		}
	}

}
