package basicplayer;

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
        		/*//SUPPLY
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
        		rc.yield();
        		
            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();

            }

		}
	}


	private static void runHQ() throws GameActionException {

		RobotInfo[] myRobots;
		
		int fate = rand.nextInt(10000);
		myRobots = rc.senseNearbyRobots(999999, myTeam);
		
		boolean spawnSuccess = false;
		
		if (rc.isWeaponReady()) {
			attackEnemyZero();
		}

		if (rc.isCoreReady() && rc.getTeamOre() >= 100 ) { //&& fate < Math.pow(1.2,12-numBeavers)*10000
			spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
		}
	}


	private static void runBeaver() throws GameActionException {
		if (rc.isWeaponReady()) {
			attackEnemyZero();
		}
		if (rc.isCoreReady()) {
			int round = Clock.getRoundNum(); //Is there a more efficient place to put this? TODO
			if ( round < 100) {
				wanderTo(rc.senseTowerLocations()[0], .5);
				goProspecting();
			}
			else {
				int fate = rand.nextInt(1000);
				if (fate < 50 && rc.getTeamOre() >= 500 && rc.getLocation().distanceSquaredTo(myHQ) > 100) {
					int numMiningFactory = rc.readBroadcast(Comms.miningfactoryCount);
					if (numMiningFactory < 5 && rc.senseOre(rc.getLocation()) > 20 ) {
						tryBuild(directions[rand.nextInt(8)],RobotType.MINERFACTORY);
					}
				} else if (fate < 600) {
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
			} else {
				rc.mine();
			}
		}
		
	}
	
	private static void goProspecting() {
		//check (the center of?) all corners of your sight
		MapLocation block = new MapLocation(0,0);
		double ore = rc.senseOre(block);
		System.out.println("ore at 0,0 is: " + ore);
	}
	
	static void attackEnemyZero() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}

    // This method will attempt to build in the given direction (or as close to it as possible)
	static void tryBuild(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.build(directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
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
