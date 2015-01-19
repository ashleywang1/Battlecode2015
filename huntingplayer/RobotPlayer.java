package huntingplayer;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
	public static int strategy; // 0 = "defend", 1 = attack (maybe choose between building drones and soldiers later
	public static int xMin, xMax, yMin, yMax;
	public static int mapXsign, mapYsign;
	public static int hqID;
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
	static RobotType[] structures = {RobotType.MINERFACTORY, RobotType.BARRACKS, RobotType.HELIPAD};
	
	static int towerThreat;
	static double navigability;
	
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
		
		mapXsign = Integer.signum(myHQ.x);
		mapYsign = Integer.signum(myHQ.y);
		
		//for the HQ only
		towerThreat = 0;
		navigability = 0;
		
		if (rc.getType() == RobotType.BEAVER && Clock.getRoundNum() < 500) {
			int assignment = rc.readBroadcast(Comms.HQtoSpawnedBeaver);
			MapLocation destination = myTowers[assignment];
			rc.broadcast(Comms.memory(rc.getID()), Map.locToInt(destination));
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
                	hqID = rc.getID();
        			runHQ();
        		} else if (rc.getType() == RobotType.BEAVER) {
        			runBeaver();
        		} else if(rc.getType()==RobotType.TOWER){
        			runTower();
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
        	
        		//SUPPLY
        		else if (rc.getType() == RobotType.SUPPLYDEPOT) {
        			Supply.runSupplyDepot();
        		}
        		
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
        		//AIR ARMY units
        		else if (rc.getType() == RobotType.HELIPAD) {
        			AirForce.runHelipad();
        		} else if (rc.getType() == RobotType.DRONE) {
        			AirForce.runDrone();
        		} else if (rc.getType() == RobotType.AEROSPACELAB) {
        			AirForce.run16Lab();
        		} else if (rc.getType() == RobotType.LAUNCHER) {
        			AirForce.runLauncher();
        		}
                
                detectEnemies();
        		transferSupplies();
        		Ore.goProspecting();
        		
        		
            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();
            }
			
			rc.yield();
		}
	}

	private static void runHQ() throws GameActionException {
		
		boolean spawnSuccess = false;
		int strategy = strategize();
        rc.broadcast(200, strategy);
		
		Attack.enemyZero();
		
		if (rc.isCoreReady()) {
			int numBeavers = rc.readBroadcast(Comms.beaverCount);
			int maxBeavers = Math.max(5, rc.readBroadcast(Comms.maxBeavers));

			if (rc.getTeamOre() >= 100 && numBeavers < maxBeavers) { //Clock.getRoundNum() < 100 && 
				spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
				if (spawnSuccess) {
					
					rc.broadcast(Comms.HQtoSpawnedBeaver, Math.min(numBeavers, myTowers.length - 1));
					rc.broadcast(Comms.beaverCount, numBeavers + 1);
				}
			} else if (rc.readBroadcast(Comms.spawnBeaver) == 1) {
				if (trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER)) {
					rc.broadcast(Comms.spawnBeaver, 0);
				}
			}
		}
		
	}
	

	
	private static int strategize() throws GameActionException {
		towerThreat = analyzeTowers();
		navigability = analyzeMap();
		int round = Clock.getRoundNum();
		int strategy = rc.readBroadcast(Comms.strategy);
		int soldiers = rc.readBroadcast(Comms.soldierCount);
		int bashers = rc.readBroadcast(Comms.basherCount);
		int tanks = rc.readBroadcast(Comms.tanksCount);
		int drones = rc.readBroadcast(Comms.droneCount);
		int casualties = rc.readBroadcast(Comms.casualties);
		int army = bashers + tanks + drones/2 - casualties;
		
		if (towerThreat >= 10 && army < (25 + 2*towerThreat) || round < 500) { //improve this formula
            strategy = 2;
//			if (navigability < .94) {
//				strategy = 2;	
//			} else {
//				strategy = 0;
//			}
            
        } else if (army > (25 + 2*towerThreat)) {
            strategy = 1;
            rc.broadcast(Comms.rushOver, 0);
            int start = rc.readBroadcast(Comms.rushStartRound);
            if (start == 0) {
            	rc.broadcast(Comms.rushStartRound, Clock.getRoundNum());
            }
        }

		//System.out.println(army + "is the army size, towerTHreat = " + towerThreat);
		//System.out.println("strategy is : " + strategy);
		
		return strategy;
	}
	
	public static double analyzeMap() {
		int round = Clock.getRoundNum();
		if (round%10 != 0) { //to save computation
			return navigability;
		}
		
        int totalNormal = 0, totalVoid = 0, totalProcessed = 1;
        MapLocation[] myBase;
        if (round < 500) {
        	myBase = MapLocation.getAllMapLocationsWithinRadiusSq(myHQ, 100);	
        } else {
        	MapLocation center = new MapLocation((myHQ.x + enemyHQ.x)/2, (myHQ.y + enemyHQ.y)/2);
        	myBase = MapLocation.getAllMapLocationsWithinRadiusSq(center, 100);
        }
        
        for (MapLocation loc : myBase) {
        	TerrainTile t = rc.senseTerrainTile(loc);

            if (t == TerrainTile.NORMAL) {
                totalNormal++;
                totalProcessed++;
            }
            else if (t == TerrainTile.VOID) {
                totalVoid++;
                totalProcessed++;
            }
            
            /*
            if (Clock.getBytecodesLeft() < 100) {
            	System.out.println((totalNormal / totalProcessed) + "is the current ratio");
                return (double) (totalNormal / totalProcessed);
            }*/
             
        }
        double ratio = (double) totalNormal / (double) totalProcessed;
        
        if (round%100 == 0) {
        	System.out.println("HQ calculation finished, " + ratio + "is the normal" + totalNormal + " and processed" + totalProcessed);	
        }
        
        
        return ratio;
    }

	//analyze how close towers are to each other
    public static int analyzeTowers() {
        MapLocation[] towers = rc.senseEnemyTowerLocations();
        int towerThreat = 0;

        for (int i=0; i< towers.length; ++i) {
            MapLocation towerLoc = towers[i];

            if ((xMin <= towerLoc.x && towerLoc.x <= xMax && yMin <= towerLoc.y && towerLoc.y <= yMax) ||
            		towerLoc.distanceSquaredTo(rc.senseEnemyHQLocation()) <= 50) {
                for (int j=0; j<towers.length; ++j) {
                    if (towers[j].distanceSquaredTo(towerLoc) <= 50) {
                        towerThreat++;
                    }
                }
            }
        }
        return towerThreat;
    }

	private static void runTower() throws GameActionException {
		Attack.lowestHP(Attack.getEnemiesInAttackingRange(RobotType.TOWER));
		int memory = rc.readBroadcast(Comms.memory(rc.getID()));
		int oldHealth = (memory == 0)? 1000: memory;
		RobotInfo[] allies = rc.senseNearbyRobots(myRange, myTeam);
		//System.out.println("tower's allies = " + allies.length);
		if (rc.getHealth() < oldHealth - 100) {
			rc.broadcast(Comms.towerDistressCall, Map.locToInt(rc.getLocation()));
			if (rc.getHealth() < 10 && navigability < .94) {
				rc.broadcast(Comms.spawnBeaver, 1); //to fix the damage
			}
		}
	}

	private static void runBeaver() throws GameActionException {
		if(rc.isCoreReady()&&Clock.getRoundNum()>1800){
			trySpawn(directions[rand.nextInt(8)], RobotType.HANDWASHSTATION);
		}
		Attack.enemyZero();
		if (rc.isCoreReady()) {
			int strategy = rc.readBroadcast(Comms.strategy);
			//airforceStrategy();
			balancedStrategy();
			//soldierStrategy();
			
			//centerStrategy(); //good for small maps
			//techStrategy(); //produces a commander
			
			//miningStrategy();
			//soldierStrategy();
			//supplyStrategy();
			
			Ore.goProspecting();
			
		}
	}
	
	//All the strategies

	private static void balancedStrategy() throws GameActionException {
		int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
		int helipadNum = rc.readBroadcast(Comms.helipadCount);
		int supplyNum = rc.readBroadcast(Comms.supplydepotCount);
		int barracks = rc.readBroadcast(Comms.barracksCount);
		int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
		if (MFnum == 0) {
			becomeHQMiningFactory(MFnum);
		} else if (helipadNum < 2) {
			becomeHelipad();
		} else if(barracks==0){
			becomeHQBarracks(barracks); //just want bashers
		} else if (TFnum < 3) {
			becomeTankFactory();
		} else if (MFnum < 2) {
			becomeMiningFactory(MFnum);
		} else if (supplyNum < 5) {
			becomeSuppliers();
		} else {
			beaverMine();
		}	
	}

	private static void airforceStrategy() throws GameActionException {
		int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
		int helipadNum = rc.readBroadcast(Comms.helipadCount);
		int supplyNum = rc.readBroadcast(Comms.supplydepotCount);
		int barracks = rc.readBroadcast(Comms.barracksCount);
		int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
		
		if (MFnum == 0) {
			becomeHQMiningFactory(MFnum);
		} else if(helipadNum ==0){
			becomeHelipad();
		}else{
			if(helipadNum-supplyNum>=2){
			becomeSuppliers();
			}else
			becomeHelipad();
		}

	}

	//purely for debugging or optimizing robot types
	private static void supplyStrategy() throws GameActionException {
		int numBarracks = rc.readBroadcast(Comms.barracksCount);
		
		if (numBarracks < Math.max(myTowers.length/2, 1)) {
			becomeBarracks();
		} else {
			becomeSuppliers();
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
		
		if (MFnum == 0) {
			becomeHQMiningFactory(MFnum);
		} else if (MFnum < 2) {
			becomeMiningFactory(MFnum);	
		} else {
			Map.randomMove();
		}
	}
	
	

	
	private static void economyStrategy() throws GameActionException {
		int round = Clock.getRoundNum(); //Is there a more efficient place to put this? TODO
		
		int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
		int numBarracks = rc.readBroadcast(Comms.barracksCount);
		int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
		int numSupplyDepots = rc.readBroadcast(Comms.supplydepotCount);
		
		//2 mining factories is optimal
		
		if (MFnum == 0) {
			becomeHQMiningFactory(MFnum);
		} else if (numBarracks < 2)  //minerfactory
			becomeBarracks();
		else if (MFnum < 2 )  //barracks
			becomeMiningFactory(MFnum);
		else if (rc.readBroadcast(Comms.supplydepotCount) < 1) {
			becomeSuppliers();
		} else if (TFnum < 4 ){
			becomeTankFactory();
		} else if (numSupplyDepots < 4) {
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
		int numBarracks = rc.readBroadcast(Comms.barracksCount);
		boolean set = false;
		/*if (!set) {
			set = 
		}*/
		setRallyCenter();	
		if (MFnum == 0) {
			becomeHQMiningFactory(MFnum);
		} else if (numBarracks < 2) {
			becomeBarracks();
		} else if (MFnum < 2 )  //barracks
			becomeMiningFactory(MFnum);
		else {
			becomeTankFactory();

		}
		
	}

	private static boolean setRallyCenter() throws GameActionException {
		MapLocation center = new MapLocation((myHQ.x + enemyHQ.x)/2, (myHQ.y + enemyHQ.y)/2);
		rc.broadcast(Comms.memory(rc.getID()), Map.locToInt(center));
		return true;
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
	
	//All beaver transformation methods

	private static void becomeMiningFactory(int numMiningFactories) throws GameActionException {
		int maxOreFound = Math.max(20, rc.readBroadcast(Comms.bestOreFieldAmount));
		int oreFieldLoc = rc.readBroadcast(Comms.bestOreFieldLoc);
		MapLocation myLoc = rc.getLocation();
		
		double block = Ore.surroundingOre(myLoc);
		if (block >= maxOreFound*9 ) {
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
	
	private static void becomeHQBarracks(int barracks) throws GameActionException {
		if(rc.getTeamOre() >=300){
			Direction away = myHQ.directionTo(rc.getLocation());
			boolean success = tryBuild(away ,RobotType.BARRACKS);
			if (success) {
				rc.broadcast(Comms.barracksCount, barracks+1);
			}
		}
		
	}

	private static void becomeBarracks() throws GameActionException {
		RobotInfo[] neighbors = rc.senseNearbyRobots(myRange);
		
		boolean spawnSuccess = false;
		
		int nearbyTowers = Map.nearbyRobots(neighbors, RobotType.TOWER);
		int nearbyBarracks = Map.nearbyRobots(neighbors, RobotType.BARRACKS);
		int dest = rc.readBroadcast(Comms.memory(rc.getID()));
		MapLocation destination = Map.intToLoc(dest);
		MapLocation myLoc = rc.getLocation();

		if (nearbyBarracks == 0 && rc.getTeamOre() >= RobotType.BARRACKS.oreCost && myLoc.distanceSquaredTo(destination) < 10) { 
			Direction toEnemy = myLoc.directionTo(enemyHQ);
			spawnSuccess = tryBuild(toEnemy,RobotType.BARRACKS);
			if (spawnSuccess) {
				int numBarracks = rc.readBroadcast(Comms.barracksCount);
				rc.broadcast(Comms.barracksCount, numBarracks + 1);
			}
		} else {
			if (myLoc.distanceSquaredTo(destination) > 10) { //move to assigned tower
				Map.tryMove(destination);
			} else {
				beaverMine();
			}
		}
		
	}

	private static void becomeTankFactory() throws GameActionException {
		int dest = rc.readBroadcast(Comms.memory(rc.getID()));
		MapLocation destination = Map.intToLoc(dest);
		MapLocation myLoc = rc.getLocation();
		RobotInfo[] neighbors = rc.senseNearbyRobots(myRange);
		if (rc.hasBuildRequirements(RobotType.TANKFACTORY) && neighbors.length < 2 && myLoc.distanceSquaredTo(destination) < 10) {
			if (rc.getTeamOre() > RobotType.TANKFACTORY.oreCost) {
				boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.TANKFACTORY);
				if (success) {
					int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
					rc.broadcast(Comms.tankfactoryCount, TFnum + 1);
				}
			} else {
				//Map.beaverMove();
				//beaverMine();
				System.out.println("This should NEVER happen");
			}
		} else {
			if (myLoc.distanceSquaredTo(destination) > 10) { //move to assigned tower
				Map.tryMove(destination);
			} else {
				beaverMine();
			}	
		}
	}
	
	private static void becomeSuppliers() throws GameActionException {
		int numSupplyDepots = rc.readBroadcast(Comms.supplydepotCount);
		if (rc.getTeamOre() >= RobotType.SUPPLYDEPOT.oreCost) {
            boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.SUPPLYDEPOT);
            if (success) {
                rc.broadcast(Comms.supplydepotCount, numSupplyDepots + 1);
            }
        }else {
			//Map.beaverMove();
        	beaverMine();
		}
        
	}
	
	private static void becomeHelipad() throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(myHQ);
		if (rc.getTeamOre() > RobotType.HELIPAD.oreCost) {
			if (tryBuild(directions[rand.nextInt(8)], RobotType.HELIPAD)) {
				int helipadNum = rc.readBroadcast(Comms.helipadCount);
				rc.broadcast(Comms.helipadCount, helipadNum + 1);
			}
		} else {
			beaverMine();
		}
	}
	

	private static void beaverMine() throws GameActionException {
		if (rc.senseOre(rc.getLocation()) > 1){
			rc.mine();	
		} else {
			Ore.minerMove(rc.getLocation());
		}
		
	}
	
	//Methods that are used by all spawning and building robots

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
	
	
	//Methods that every robot will use
	
	private static void transferSupplies() throws GameActionException {
	    boolean isHQ = rc.getType() == RobotType.HQ;
	    boolean isBeaver = rc.getType() == RobotType.BEAVER;
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
        double lowestSupply = rc.getSupplyLevel();
        double transferAmount = rc.getSupplyLevel();
        MapLocation suppliesToThisLocation = null;
        for(RobotInfo ri:nearbyAllies){
            if (ri.type == RobotType.TOWER || ri.type == RobotType.HQ || 
            		(!isHQ && ri.type == RobotType.BEAVER)) //|| (isBeaver && ri.supplyLevel > 10)
                continue;
            if(ri.supplyLevel<lowestSupply){
                lowestSupply = ri.supplyLevel;
                if (!isHQ) {
                	transferAmount = (rc.getSupplyLevel()-ri.supplyLevel)/2;
                } else {
                	transferAmount = rc.getSupplyLevel();
                }
                    
                suppliesToThisLocation = ri.location;
            }
        }
        if(suppliesToThisLocation!=null && transferAmount > 0){
            rc.transferSupplies((int)transferAmount, suppliesToThisLocation);
        }
    }
	
	private static void detectEnemies() throws GameActionException {
		RobotType type = rc.getType();
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		MapLocation myLoc = rc.getLocation();
		
		if (Clock.getRoundNum() < 500) {
			int earlyRally = rc.readBroadcast(Comms.defensiveRally);
			if (enemies.length > 0 && earlyRally == 0) {
				//broadcast that we're on the FULL defensive TODO
				//rc.broadcast(Comms.strategy, 3);
				//rc.broadcast(Comms.defensiveRally, Map.locToInt(enemies[0].location));
			}
		}
		if (type == RobotType.MINER && rc.readBroadcast(Comms.enemiesNearMiners) == 0 && enemies.length > 3) {
			rc.broadcast(Comms.enemiesNearMiners, Map.locToInt(myLoc));
		} else if (type == RobotType.DRONE && enemies.length > 0) {
			if (enemies.length > 3) {
				//System.out.println("Drone found more than 3 enemies. Run away?");
				rc.broadcast(Comms.droneRallyPoint, Map.locToInt(myLoc));
			} else {
				for (RobotInfo target: enemies) {
					
					for (RobotType struct: structures) {
						if (target.type == struct) {
							rc.broadcast(Comms.droneTarget, Map.locToInt(target.location));
							continue;
						}
					}
				}
			}
		}
		/* RobotType type = rc.getType();
		if (rc.getHealth() < 10) {
			System.out.println("SOLDIER DOWN SOLDIER DOWN" + rc.getID());
			int deaths = rc.readBroadcast(Comms.casualties);
			rc.broadcast(Comms.casualties, deaths + 1);
		} //I haven't used this yet TODO
		*/
	}

}
