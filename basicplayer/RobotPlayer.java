package basicplayer;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
	static RobotController rc;
	static Team myTeam;
	static MapLocation myHQ;
	static MapLocation[] myTowers;
	static Team enemyTeam;
	static MapLocation enemyHQ;
	static MapLocation[] enemyTowers;
	
	static int myRange;
	static Direction facing;
	
	static Random rand;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	
	public static void run(RobotController RC) throws GameActionException {
		rc = RC;
        rand = new Random(rc.getID());

		myRange = rc.getType().attackRadiusSquared;
		facing = Direction.values()[(int)(rand.nextDouble()*8)];
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
        Direction lastDirection = null;
		myTeam = rc.getTeam();
		myHQ = rc.senseHQLocation();
		myTowers = rc.senseTowerLocations();
		enemyTeam = myTeam.opponent();
		enemyHQ = rc.senseEnemyHQLocation();
		enemyTowers = rc.senseEnemyTowerLocations();
		
		if (rc.getType() == RobotType.BEAVER) {
			int assignment = rc.readBroadcast(Comms.HQtoSpawnedBeaver);
			rc.broadcast(Comms.memory(rc.getID()), assignment);
			System.out.println(rc.getID()%10000 + " is my own personall channel id");
		}
		
				
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
        		}else if(rc.getType()==RobotType.TOWER){
        			attackEnemyZero();
        		}
        		
        		//ARMY
        		//GROUND ARMY units
        		else if (rc.getType() == RobotType.BARRACKS) {
        			Army.runBarracks();
        		} else if (rc.getType() == RobotType.SOLDIER) {
        			Army.runSoldier();
        		} else if (rc.getType() == RobotType.BASHER) {
        			Army.runBasher();
        		} else if (rc.getType() == RobotType.TANKFACTORY) {
        			Army.runTankFactory();
        		} else if (rc.getType() == RobotType.TANK) {
        			Army.runTank();
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
        		
        		/AIR ARMY units
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
        		
        		
            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();

            }
			
			rc.yield();

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
			if (Clock.getRoundNum() < 500 && rc.getTeamOre() >= 100 && numBeavers < 5) {
				spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
				if (spawnSuccess) {
					rc.broadcast(Comms.HQtoSpawnedBeaver, 7); //make it a minerfactory
					rc.broadcast(Comms.beaverCount, numBeavers + 1);
				}
			} else if (rc.getTeamOre() >= 100 && numBeavers < 15){
				spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
				if (spawnSuccess) {
					
					rc.broadcast(Comms.HQtoSpawnedBeaver, rand.nextInt(myTowers.length)); //make it a minerfactory
					rc.broadcast(Comms.beaverCount, numBeavers + 1);
				}
			}
		}
		
	}


	private static void runBeaver() throws GameActionException {
		if (rc.isWeaponReady()) {
			attackEnemyZero();
		}
		if (rc.isCoreReady()) {
			int round = Clock.getRoundNum(); //Is there a more efficient place to put this? TODO
			
			int numMiningFactories = rc.readBroadcast(Comms.miningfactoryCount);
			int numBarracks = rc.readBroadcast(Comms.barracksCount);
			int maxOreFound = rc.readBroadcast(Comms.bestOreFieldAmount);
			//2 mining factories is optimal
			int assignment = rc.readBroadcast(Comms.memory(rc.getID()));
			
			Direction toEnemy = rc.getLocation().directionTo(enemyHQ);
			
			if (numMiningFactories < 2 && assignment == 7) { //minerfactory
				if (rc.getTeamOre() >= 500 && numMiningFactories < 2) {
					if (rc.senseOre(rc.getLocation()) >= maxOreFound ) {
						boolean success = tryBuild(directions[rand.nextInt(8)],RobotType.MINERFACTORY);
						if (success) {
							rc.broadcast(Comms.miningfactoryCount, numMiningFactories + 1);
						}
					}
				} else if (rand.nextDouble() < .5 && rc.senseOre(rc.getLocation())>=20){
					rc.mine();
				} else {

					int oreFieldLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
					if (oreFieldLoc != 0) {
						Map.tryMove(Map.intToLoc(oreFieldLoc));
					} else {
						Direction awayFromHQ = myHQ.directionTo(rc.getLocation());
						Map.wanderToward(awayFromHQ, .5);
					}

				}
				
			} else if (numBarracks < 5) { //barracks
				RobotInfo[] neighbors = rc.senseNearbyRobots(myRange);
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
				if (nearbyBarracks.size() == 0 && rc.getTeamOre() >= 300) {
					if (nearbyTowers.size() > 0 || Clock.getRoundNum() > 400) {
						tryBuild(toEnemy,RobotType.BARRACKS);	
					}
				} else {
					//move to assigned tower
					int n = rc.readBroadcast(Comms.memory(rc.getID()));
					Map.tryMove(myTowers[n]);					
				}	
			} else {
				if (rc.getTeamOre() > RobotType.TANKFACTORY.oreCost) {
					//add dependency condition TODO
					tryBuild(toEnemy, RobotType.TANKFACTORY);
					//add count to Comms TODO
				}
			}
			
			
			goProspecting();
		}
	}

	private static void runMinerFactory() throws GameActionException {
		
		boolean success = false;
		int numMiners = rc.readBroadcast(Comms.minerCount);
		if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.MINER.oreCost && numMiners < 100) {
			int oreFields = rc.readBroadcast(Comms.bestOreFieldLoc);
			
			if (oreFields ==0) {
				success = trySpawn(directions[rand.nextInt(8)], RobotType.MINER);
			} else {
				MapLocation oreLoc = Map.intToLoc(oreFields);
				success = trySpawn(rc.getLocation().directionTo(oreLoc), RobotType.MINER);
			}
			
			//broadcast and update numMiners
			if (success) {
				rc.broadcast(Comms.minerCount, numMiners + 1);			
			}
		}
		
		//if under attack, broadcast for help
		if (rc.getHealth() < 20) {
			int numMF = rc.readBroadcast(Comms.miningfactoryCount);
			rc.broadcast(Comms.miningfactoryCount, numMF - 1);
		}
	}


	private static void runMiner() throws GameActionException {
		
		MapLocation spawnPoint;
		RobotInfo[] enemies;
		RobotInfo[] allies;
		
		enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		allies = rc.senseNearbyRobots(myRange-2, myTeam);
		//avoid enemies, tolerate allies to 3
		//walls = rc.senseTerrainTile(rc.getLocation());
		
		if (rc.isCoreReady()) {
			MapLocation myLoc = rc.getLocation();
			MapLocation miner = nearestMiner(allies);
			if (allies.length < 3 && miner!=null) { //if you're not crowded
				
				Direction away = miner.directionTo(myLoc);
				if (rand.nextDouble() < .8) {
					Map.tryMove(away); //move away from others					
				} else if (rand.nextDouble() < .5) {
					Map.tryMove(away.rotateLeft());
				} else {
					Map.tryMove(away.rotateRight());
				}
				
			} else if (enemies.length > 0) {
				Map.tryMove(enemies[0].location.directionTo(myLoc)); //move away from others
			} else if (rc.senseOre(myLoc) > 12) {
				rc.mine();
			} else {
				int oreLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
				double front = rc.senseOre(myLoc.add(facing));
				double right = rc.senseOre(myLoc.add(facing.rotateRight()));
				double left = rc.senseOre(myLoc.add(facing.rotateLeft()));
				if (front > 12) {
					Map.tryMove(facing);
				} else if (oreLoc == 0 || Clock.getRoundNum() < 300) {
					if (right > left) {
						Map.tryMove(facing.rotateRight());
					}
					else {
						Map.tryMove(facing.rotateLeft());
					}	
				} else {
					MapLocation oreField = Map.intToLoc(oreLoc);
					Map.tryMove(myLoc.directionTo(oreField));
				}
				
			}
		}
		
	}
	
	private static MapLocation nearestMiner(RobotInfo[] allies) {
		for (RobotInfo ri: allies) {
			if (ri.type == RobotType.MINER) {
				return ri.location;
			}
		}
		return null;
	}



	public static void goProspecting() throws GameActionException {
		//TODO eventually don't just judge one square, make it all 16 squares in range
		//eventually once this region dries up, change the best ore field
		
		int maxOre = Math.max(rc.readBroadcast(Comms.bestOreFieldAmount), 20);
		double ore = rc.senseOre(rc.getLocation());
		int loc = rc.readBroadcast(Comms.bestOreFieldLoc);
		MapLocation mapCoords = Map.intToLoc(loc);
		MapLocation myLoc = rc.getLocation();
		
		if (ore > maxOre) {
			
			int coords = Map.locToInt(rc.getLocation());			
			System.out.println("HEY FOUND BETTER" + coords); //TODO
			rc.broadcast(Comms.bestOreFieldLoc, coords);
			rc.broadcast(Comms.bestOreFieldAmount, (int) ore);
		} else if (myLoc.equals(mapCoords) && rc.senseOre(myLoc) < 12) {
			rc.broadcast(Comms.bestOreFieldAmount, 0);
			rc.broadcast(Comms.bestOreFieldLoc, 0);
		}
		
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
		int dirint = Map.directionToInt(d);
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
	
	
	// This method will attempt to spawn in the given direction (or as close to it as possible)
	static boolean trySpawn(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = Map.directionToInt(d);
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
	    boolean isHQ = rc.getType() == RobotType.HQ;
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
        double lowestSupply = rc.getSupplyLevel();
        double transferAmount = rc.getSupplyLevel();
        MapLocation suppliesToThisLocation = null;
        for(RobotInfo ri:nearbyAllies){
            if(ri.supplyLevel<lowestSupply){
                lowestSupply = ri.supplyLevel;
                if (!isHQ)
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
