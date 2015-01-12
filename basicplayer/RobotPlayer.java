package basicplayer;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
	public static int strategy; // 0 = "defend", 1 = attack (maybe choose between building drones and soldiers later
	static int towerThreat;
	public static int xMin, xMax, yMin, yMax;
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
			//System.out.println(rc.getID()%10000 + " is my own personall channel id");
		}
		
		int round = Clock.getRoundNum();
		if (round < 1) {
		    rc.broadcast(Comms.lowestBarracksSupply, 10000);
	        rc.broadcast(Comms.lowestMiningFactorySupply, 10000);
		    rc.broadcast(Comms.lowestSoldierSupply, 10000);
		    rc.broadcast(Comms.lowestMinerSupply, 10000);
		}
				
		while(true) {
			try {
				//information
                rc.setIndicatorString(0, "This is an indicator string.");
                rc.setIndicatorString(1, "I am a " + rc.getType());
                
                if (rc.getType() == RobotType.HQ) { //MAIN
        			runHQ();
        		} else if (rc.getType() == RobotType.BEAVER) {
        			runBeaver();
        		} else if(rc.getType()==RobotType.TOWER){
        			Attack.lowestHP(Attack.getEnemiesInAttackingRange(RobotType.TOWER));
        		} else if (rc.getType() == RobotType.MINERFACTORY) { //MINING units
        			Ore.runMinerFactory();
        		} else if (rc.getType() == RobotType.MINER) {
        			Ore.runMiner();
        		} else if (rc.getType() == RobotType.BARRACKS) { //GROUND ARMY units
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
        		} */
        		//TECHNOLOGY ARMY units
        		else if (rc.getType() == RobotType.TECHNOLOGYINSTITUTE) {
        			Tech.runMIT();
        		} else if (rc.getType() == RobotType.COMPUTER) {
        			Tech.runComputer();
        		} else if (rc.getType() == RobotType.TRAININGFIELD) {
        			Tech.runTrainingField();
        		} else if (rc.getType() == RobotType.COMMANDER) {
        			Tech.runCommander();
        		}
        		/*
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
        		analyzeTowers();
        		if(Clock.getRoundNum()>1000)
        		chooseStrategy();
        		
            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();
            }
			
			rc.yield();
		}
	}

	private static void runHQ() throws GameActionException {
		
		boolean spawnSuccess = false;
		int strategy = Map.strategize();
		
		Attack.enemyZero();
		if (rc.isCoreReady()) {
			int numBeavers = rc.readBroadcast(Comms.beaverCount);
			int maxBeavers = Math.max(5, rc.readBroadcast(Comms.maxBeavers));

			if (Clock.getRoundNum() < 100 && rc.getTeamOre() >= 100 && numBeavers < maxBeavers) {
				spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
				if (spawnSuccess) {
					//int assignment = rc.readBroadcast(Comms.HQtoSpawnedBeaver);
					rc.broadcast(Comms.HQtoSpawnedBeaver, numBeavers);
					rc.broadcast(Comms.beaverCount, numBeavers + 1);
				}
			}
			/*else if (rc.getTeamOre() >= 100 && numBeavers < 10){
				spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
				if (spawnSuccess) {
					
					rc.broadcast(Comms.HQtoSpawnedBeaver, rand.nextInt(myTowers.length));
					rc.broadcast(Comms.beaverCount, numBeavers + 1);
				}
			}*/
		}
		
	}


	private static void runBeaver() throws GameActionException {
		Attack.enemyZero();
		if (rc.isCoreReady()) {
			economyStrategy();
			//techStrategy();
			//basicStrategy();
			//miningStrategy();
			//soldierStrategy();
		}
	}

	private static void soldierStrategy() throws GameActionException {
		
		int numBarracks = rc.readBroadcast(Comms.barracksCount);
		
		if (numBarracks < 6) {
			becomeBarracks();
		}
	}

	private static void miningStrategy() throws GameActionException {
		int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
		
		if (MFnum < 2) {
			becomeMiningFactory(MFnum);	
		}
	}

	private static void basicStrategy() throws GameActionException {
		int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
		int numBarracks = rc.readBroadcast(Comms.barracksCount);
		int numSupplyDepots = rc.readBroadcast(Comms.supplydepotCount);
		
		if (MFnum < 2) {
			becomeMiningFactory(MFnum);
		} else if (numSupplyDepots == 0) {
		    becomeSuppliers();
		} else if (numBarracks < 6) {
			becomeBarracks();
		} else {
		    becomeSuppliers();
		}
	}
	
	private static void centerStrategy() throws GameActionException {
		// good for the shield map
		// create tons of beavers, send to center of the map, beaverMine then barracks
		//1 MF right next to HQ
		// shut down enemy soldiers
		rc.broadcast(Comms.maxBeavers, 20);
		int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
		
		if (MFnum == 0) {
			becomeHQMiningFactory(MFnum);
		}
		
	}

	private static void techStrategy() throws GameActionException {
		int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
		int numBarracks = rc.readBroadcast(Comms.barracksCount);
		int MIT = rc.readBroadcast(Comms.MITCount);
		int TFnum = rc.readBroadcast(Comms.TrainingFieldCount);
		
		if (MFnum < 2) {
			becomeMiningFactory(MFnum);	
		} else if (numBarracks < 2) {
			becomeBarracks();
		} else if (MIT == 0 && rc.getTeamOre() > RobotType.TECHNOLOGYINSTITUTE.oreCost) {
			boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.TECHNOLOGYINSTITUTE);
			if (success) {
				rc.broadcast(Comms.MITCount, 1);
			}
		} else if (MIT == 1 && TFnum == 0 && rc.getTeamOre() > RobotType.TRAININGFIELD.oreCost) {
			boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.TRAININGFIELD);
			if (success) {
				rc.broadcast(Comms.TrainingFieldCount, 1);
			}
		}
		else {
			beaverMine();
		}
		
	}

	private static void economyStrategy() throws GameActionException {
		int round = Clock.getRoundNum(); //Is there a more efficient place to put this? TODO
		
		int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
		int numBarracks = rc.readBroadcast(Comms.barracksCount);
		
		//2 mining factories is optimal
		int assignment = rc.readBroadcast(Comms.memory(rc.getID()));
		
		if (MFnum == 0) {
			becomeHQMiningFactory(MFnum);
		} else if (numBarracks < 2)  //minerfactory
			becomeBarracks();
		else if (MFnum < 2 )  //barracks
			becomeMiningFactory(MFnum);
		else {
			if (rc.getTeamOre() > RobotType.TANKFACTORY.oreCost) {
				//add dependency condition TODO
				tryBuild(directions[rand.nextInt(8)], RobotType.TANKFACTORY);
				//add count to Comms TODO
			}
		}
		
		Ore.goProspecting();
		
	}

	private static void becomeBarracks() throws GameActionException {
		RobotInfo[] neighbors = rc.senseNearbyRobots(myRange);
		
		boolean spawnSuccess = false;
		
		int nearbyTowers = Map.nearbyRobots(neighbors, RobotType.TOWER);
		int nearbyBarracks = Map.nearbyRobots(neighbors, RobotType.BARRACKS);

		if (nearbyTowers > 0 && nearbyBarracks == 0 && rc.getTeamOre() >= 300) {
			Direction toEnemy = rc.getLocation().directionTo(enemyHQ);
			spawnSuccess = tryBuild(toEnemy,RobotType.BARRACKS);
			if (spawnSuccess) {
				int numBarracks = rc.readBroadcast(Comms.barracksCount);
				rc.broadcast(Comms.barracksCount, numBarracks + 1);
			}
		} else {
			int n = rc.readBroadcast(Comms.memory(rc.getID()));
			if (rc.getLocation().distanceSquaredTo(myTowers[n]) > 10) { //move to assigned tower
				Map.tryMove(myTowers[n]);
			} else {
				beaverMine();
			}
		}
		
	}


	private static void becomeMiningFactory(int numMiningFactories) throws GameActionException {
		int maxOreFound = Math.max(20, rc.readBroadcast(Comms.bestOreFieldAmount));
		int oreFieldLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
		MapLocation myLoc = rc.getLocation();
		
		double block = Ore.surroundingOre(myLoc);
		//System.out.println(maxOreFound*9 + "is the max ore found???");
		//System.out.println(block + "at location " + rc.getLocation());
		if (block >= maxOreFound*9 ) {
			//boolean noMFnearby = false;
			RobotInfo[] neighbors = rc.senseNearbyRobots(myRange);
			int nearbyMF = Map.nearbyRobots(neighbors, RobotType.MINERFACTORY);
			
			if (rc.getTeamOre() >= 500 && nearbyMF == 0) {
				boolean success = tryBuild(directions[rand.nextInt(8)],RobotType.MINERFACTORY);
				if (success) {
					rc.broadcast(Comms.miningfactoryCount, numMiningFactories + 1);
				}	
			} else {
				beaverMine();		
			}
		} else {
			if (oreFieldLoc != 0) {
				Map.tryMove(Map.intToLoc(oreFieldLoc));
			} else {
				Direction awayFromHQ = myHQ.directionTo(rc.getLocation());
				Map.wanderToward(awayFromHQ, .7);
			}
		}
	}
	
	private static void becomeHQMiningFactory(int numMiningFactories) throws GameActionException {
		if (rc.getTeamOre() >= 500) {
			Direction away = myHQ.directionTo(rc.getLocation());
			boolean success = tryBuild(away ,RobotType.MINERFACTORY);
			if (success) {
				rc.broadcast(Comms.miningfactoryCount, numMiningFactories + 1);
			}	
		} else {
			beaverMine();		
		}
	}
	
	private static void becomeSuppliers() throws GameActionException {
	    if (rc.isCoreReady()) {
	        int numSupplyDepots = rc.readBroadcast(Comms.supplydepotCount);
	        if (rc.getSupplyLevel() < 100) {
	            Map.tryMove(myHQ);
	        } else {
	            if (numSupplyDepots < 2 && rc.getTeamOre() >= 100) {
	                boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.SUPPLYDEPOT);
                    if (success) {
                        rc.broadcast(Comms.supplydepotCount, numSupplyDepots + 1);
                    }
	            } else if (rc.readBroadcast(Comms.lowestBarracksSupply) < 100) {
	                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestBarracksSupplyLoc));
	                Map.tryMove(lowestSupplyLoc);
	            } else if (rc.readBroadcast(Comms.lowestSoldierSupply) < 30) {
	                MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestSoldierSupplyLoc));
	                Map.tryMove(lowestSupplyLoc);
	                //System.out.println(rc.readBroadcast(Comms.lowestSoldierSupply) + " go to soldiers");
	            } else if (rc.readBroadcast(Comms.lowestMinerSupply) < 30) {
                    MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestMinerSupplyLoc));
                    Map.tryMove(lowestSupplyLoc);
                    //System.out.println(rc.readBroadcast(Comms.lowestMinerSupply) + " go to miners");
                } else if (rc.readBroadcast(Comms.lowestMiningFactorySupply) < 100) {
                    MapLocation lowestSupplyLoc = Map.intToLoc(rc.readBroadcast(Comms.lowestMiningFactorySupplyLoc));
                    Map.tryMove(lowestSupplyLoc);
	            } else if (rc.getTeamOre() >= 100) {
	                boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.SUPPLYDEPOT);
	                if (success) {
	                    rc.broadcast(Comms.supplydepotCount, numSupplyDepots + 1);
	                }
	            }
	        }
	    }
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
	    if (avgSupply < 30 && avgSupply < rc.readBroadcast(supplyChannel)) {
	        rc.broadcast(supplyChannel, (int) avgSupply);
	        rc.broadcast(supplyLocChannel, Map.locToInt(currentLoc));
            //System.out.println(avgSupply + " lowest avg supply");
	    } else if (avgSupply > 30 && rc.readBroadcast(supplyLocChannel) == Map.locToInt(currentLoc)) {
	        rc.broadcast(supplyChannel, 10000);
	    }
	}

	private static void beaverMine() throws GameActionException {
		if (rc.senseOre(rc.getLocation()) > 4){
			rc.mine();	
		} else {
			Map.randomMove();
			//or detect good ore and move there
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
	    boolean isBeaver = rc.getType() == RobotType.BEAVER;
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
        double lowestSupply = rc.getSupplyLevel();
        double transferAmount = rc.getSupplyLevel();
        MapLocation suppliesToThisLocation = null;
        for(RobotInfo ri:nearbyAllies){
            if (ri.type == RobotType.TOWER || ri.type == RobotType.HQ || (!isHQ && ri.type == RobotType.BEAVER) || (isBeaver && ri.supplyLevel > 10))
                continue;
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
	
    //analyze how close towers are to each other
    public static void analyzeTowers() {
        MapLocation[] towers = rc.senseEnemyTowerLocations();
        towerThreat = 0;

        for (int i=0; i<towers.length; ++i) {
            MapLocation towerLoc = towers[i];

            if ((xMin <= towerLoc.x && towerLoc.x <= xMax && yMin <= towerLoc.y && towerLoc.y <= yMax) || towerLoc.distanceSquaredTo(rc.senseEnemyHQLocation()) <= 50) {
                for (int j=0; j<towers.length; ++j) {
                    if (towers[j].distanceSquaredTo(towerLoc) <= 50) {
                        towerThreat++;
                    }
                }
            }
        }
    }
    
    //choose strategy based on tower threat
    public static void chooseStrategy() throws GameActionException {
        if (towerThreat >= 10) {
            //play defensive
            strategy = 0;
        }
        else {
            strategy = 1;
            }
        
        rc.broadcast(200, strategy);
    }

}
