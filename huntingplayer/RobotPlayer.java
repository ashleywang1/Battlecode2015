package huntingplayer;

import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
    public static int strategy;
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
        } else if (rc.getType() == RobotType.TANK) {
            int status = rc.readBroadcast(Comms.TFtoSpawnedTank);
            if (status == 1) {
                //become a defender
                rc.broadcast(Comms.memory(rc.getID()), 1);
                rc.broadcast(Comms.TFtoSpawnedTank, 0);
            }

        }

        int round = Clock.getRoundNum();
        if (round < 1) {
            rc.broadcast(Comms.lowestBarracksSupply, 10000);
            rc.broadcast(Comms.lowestMiningFactorySupply, 10000);
            rc.broadcast(Comms.lowestBasherSupply, 10000);
            rc.broadcast(Comms.lowestMinerSupply, 10000);
            rc.broadcast(Comms.lowestTankSupply, 10000);
            rc.broadcast(Comms.lowestHelipadSupply, 10000);
            rc.broadcast(Comms.lowestTankFactorySupply, 10000);
        }

        while(true) {
            try {
                //information
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
                } else if(rc.getType() == RobotType.MISSILE){
                    AirForce.runMissile();
                }

                if(rc.getType()!=RobotType.MISSILE){
                    detectEnemies();
                    transferSupplies();
                    Ore.goProspecting();
                }



            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();
            }

            rc.yield();
        }
    }

    private static void runHQ() throws GameActionException {

        boolean spawnSuccess = false;

        Attack.enemyZero();
        updateRobotCount();

        if (rc.isCoreReady()) {
            //RobotInfo[] all = rc.senseNearbyRobots();


            int numBeavers = rc.readBroadcast(Comms.beaverCount);
            int maxBeavers = Math.max(3, rc.readBroadcast(Comms.maxBeavers));

            if (rc.getTeamOre() >= RobotType.BEAVER.oreCost && numBeavers < maxBeavers) { 
                spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
                if (spawnSuccess) {

                    rc.broadcast(Comms.HQtoSpawnedBeaver, Math.min(numBeavers, myTowers.length - 1));
                    //rc.broadcast(Comms.beaverCount, numBeavers + 1);
                }
            } else if (rc.readBroadcast(Comms.spawnBeaver) == 1) {
                if (trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER)) {
                    rc.broadcast(Comms.spawnBeaver, 0);
                }
            }
        }

    }

    private static void updateRobotCount() throws GameActionException { //This method uses about 2000 bytecode
        RobotInfo[] allMine = rc.senseNearbyRobots(999999, myTeam);

        int beaver = 0;
        int miner = 0;
        int barracks = 0;
        int soldiers = 0;
        int bashers = 0;
        int tankFactory = 0;
        int tank = 0;
        int helipad = 0;
        int drones = 0;
        int supplyDepot = 0;
        int aeroLab = 0;
        int launcher = 0;
        int[] channels = {Comms.beaverCount, Comms.minerCount, Comms.barracksCount, Comms.soldierCount, Comms.basherCount,
                Comms.tankfactoryCount, Comms.tanksCount, Comms.helipadCount, Comms.droneCount, Comms.supplydepotCount,
                Comms.aerospacelabCount, Comms.launcherCount};

        for (RobotInfo x: allMine) {
            if (x.type == RobotType.BEAVER) {
                beaver +=1;
            } else if (x.type == RobotType.MINER) {
                miner +=1;
            } else if (x.type == RobotType.BARRACKS) {
                barracks +=1;
            } else if (x.type == RobotType.SOLDIER) {
                soldiers +=1;
            } else if (x.type == RobotType.BASHER) {
                bashers +=1;
            } else if (x.type == RobotType.TANKFACTORY) {
                tankFactory +=1;
            } else if (x.type == RobotType.TANK) {
                tank +=1;
            } else if (x.type == RobotType.HELIPAD) {
                helipad +=1;
            } else if (x.type == RobotType.DRONE) {
                drones +=1;
            } else if (x.type == RobotType.SUPPLYDEPOT) {
                supplyDepot +=1;
            }  else if (x.type == RobotType.AEROSPACELAB) {
                aeroLab +=1;
            }  else if (x.type == RobotType.LAUNCHER) {
                launcher +=1;
            } 
        }

        int[] rollCall = {beaver, miner, barracks, soldiers, bashers, tankFactory, tank, helipad, drones, supplyDepot, aeroLab,
                launcher};

        for (int i=0; i < rollCall.length; i++) {
            rc.broadcast(channels[i], rollCall[i]);
        }

    }

    private static void runTower() throws GameActionException {
        Attack.lowestHP(Attack.getEnemiesInAttackingRange(RobotType.TOWER));
        int memory = rc.readBroadcast(Comms.memory(rc.getID()));
        int oldHealth = (memory == 0)? 1000: memory;
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

        if (rc.isCoreReady()) {
            //tankStrategy();
            launcherStrategy();	
        }
        Attack.enemyZero();
    }


    private static void tankStrategy() throws GameActionException {
        int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
        int helipadNum = rc.readBroadcast(Comms.helipadCount);
        int supplyNum = rc.readBroadcast(Comms.supplydepotCount);
        int barracks = rc.readBroadcast(Comms.barracksCount);
        int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
        double myOre = rc.getTeamOre();

        if (MFnum == 0) {
            becomeHQMiningFactory(MFnum);
        } else if(barracks==0){
            becomeHQBarracks(barracks); //just want bashers
        } else if (TFnum == 0) {
            becomeHQTankFactory();
        } else if (TFnum < 6 || myOre > 3000) {
            becomeHQTankFactory();
        } else if (helipadNum < 1 ) {
            becomeHelipad();
        } else if (MFnum < 2) {
            becomeMiningFactory(MFnum);
        } else if (supplyNum < 5) {
            becomeSuppliers();
        } 
    }

    private static void launcherStrategy() throws GameActionException {
        int MFnum = rc.readBroadcast(Comms.miningfactoryCount);
        int helipadNum = rc.readBroadcast(Comms.helipadCount);
        int supplyNum = rc.readBroadcast(Comms.supplydepotCount);
        int aeroNum = rc.readBroadcast(Comms.aerospacelabCount);
        int barracks = rc.readBroadcast(Comms.barracksCount);
        int TFnum = rc.readBroadcast(Comms.tankfactoryCount);

        if (MFnum == 0) {
            becomeHQMiningFactory(MFnum);
        } else if (helipadNum < 1) {
            becomeHelipad();
        }	else if(aeroNum<=3 ){
            becomeAeroLab();
        }
        else if(aeroNum>3 && supplyNum<2){
            becomeSuppliers();
        }else if(aeroNum<6){
            becomeAeroLab();
        }else if (supplyNum<10)
            becomeSuppliers();

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
        if(rc.getTeamOre() >= RobotType.BARRACKS.oreCost){
            Direction away = myHQ.directionTo(rc.getLocation());
            boolean success = tryBuild(away ,RobotType.BARRACKS);
            if (success) {
                rc.broadcast(Comms.barracksCount, barracks+1);
            }
        }

    }

    private static void becomeHQTankFactory() throws GameActionException {
        MapLocation myLoc = rc.getLocation();

        if (rc.hasBuildRequirements(RobotType.TANKFACTORY) && myLoc.distanceSquaredTo(myHQ) < 5) {
            boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.TANKFACTORY);
            if (success) {
                int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
                rc.broadcast(Comms.tankfactoryCount, TFnum + 1);
            }
        } else {
            Map.tryMove(myHQ);
        }
    }

    private static void becomeTankFactory() throws GameActionException {
        int dest = rc.readBroadcast(Comms.memory(rc.getID()));
        MapLocation destination = Map.intToLoc(dest);
        MapLocation myLoc = rc.getLocation();
        RobotInfo[] neighbors = rc.senseNearbyRobots(myRange);

        if (rc.hasBuildRequirements(RobotType.TANKFACTORY) && neighbors.length < 3 && myLoc.distanceSquaredTo(destination) < 10) {
            if (rc.getTeamOre() > RobotType.TANKFACTORY.oreCost) {
                boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.TANKFACTORY);
                if (success) {
                    int TFnum = rc.readBroadcast(Comms.tankfactoryCount);
                    rc.broadcast(Comms.tankfactoryCount, TFnum + 1);
                }
            }
        } else if (myLoc.distanceSquaredTo(destination) > 10) { //move to assigned tower
            Map.tryMove(destination);
        } else {
            beaverMine();

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
            Map.beaverMove();
            //beaverMine();
        }

    }

    private static void becomeHelipad() throws GameActionException {
        if (rc.getTeamOre() > RobotType.HELIPAD.oreCost) {
            if (tryBuild(directions[rand.nextInt(8)], RobotType.HELIPAD)) {
                int helipadNum = rc.readBroadcast(Comms.helipadCount);
                rc.broadcast(Comms.helipadCount, helipadNum + 1);
            }
        } else {
            beaverMine();
        }
    }

    private static void becomeAeroLab() throws GameActionException {
        int dest = rc.readBroadcast(Comms.memory(rc.getID()));
        MapLocation destination = Map.intToLoc(dest);
        MapLocation myLoc = rc.getLocation();
        RobotInfo[] neighbors = rc.senseNearbyRobots(myRange);
        if (rc.hasBuildRequirements(RobotType.AEROSPACELAB)) {
            if (rc.getTeamOre() > RobotType.AEROSPACELAB.oreCost) {
                boolean success = tryBuild(directions[rand.nextInt(8)], RobotType.AEROSPACELAB);
                if (success) {
                    int aeroNum= rc.readBroadcast(Comms.aerospacelabCount);
                    rc.broadcast(Comms.aerospacelabCount, aeroNum + 1);
                }
            }
        } else {
            Map.beaverMove();
            //beaverMine();
            //Map.tryMove(destination);
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
        boolean isHQOrSupplyDepot = rc.getType() == RobotType.HQ || rc.getType() == RobotType.SUPPLYDEPOT;
        boolean isDrone = rc.getType() == RobotType.DRONE;
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,rc.getTeam());
        double lowestSupply = rc.getSupplyLevel();
        double transferAmount = rc.getSupplyLevel();
        MapLocation suppliesToThisLocation = null;
        for(RobotInfo ri:nearbyAllies){
            if (ri.type == RobotType.TOWER || ri.type == RobotType.HQ || 
                    (!isHQOrSupplyDepot && ri.type == RobotType.DRONE) || (isBuilding(ri.type) && ri.supplyLevel > 300)) //|| (isBeaver && ri.supplyLevel > 10)
                continue;
            if(ri.supplyLevel<lowestSupply){
                lowestSupply = ri.supplyLevel;
                if (!isHQOrSupplyDepot) {
                    transferAmount = (rc.getSupplyLevel()-ri.supplyLevel)/2;
                } else {
                    transferAmount = rc.getSupplyLevel();
                }

                suppliesToThisLocation = ri.location;
            }
        }
        try{
            if(suppliesToThisLocation!=null && transferAmount > 0){
                rc.transferSupplies((int)transferAmount, suppliesToThisLocation);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static boolean isBuilding(RobotType type) {
        return type == RobotType.BARRACKS || type == RobotType.MINERFACTORY || type == RobotType.HELIPAD;
    }

    private static void detectEnemies() throws GameActionException {
        RobotType type = rc.getType();
        RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
        MapLocation myLoc = rc.getLocation();

        if (Clock.getRoundNum() < 500) {
            int earlyRally = rc.readBroadcast(Comms.defensiveRally);
            if (enemies.length > 0 && earlyRally == 0) {
                //broadcast that we're on the FULL defensive TODO
                rc.broadcast(Comms.defensiveRally, Map.locToInt(enemies[0].location));
                System.out.println("We are being attacked early!!");
            }
        }

        if (type == RobotType.MINER && rc.readBroadcast(Comms.enemiesNearMiners) == 0 && enemies.length > 3) {
            rc.broadcast(Comms.enemiesNearMiners, Map.locToInt(myLoc));
        } else if (type == RobotType.DRONE && enemies.length > 0) {
            if (enemies.length > 3) {
                //System.out.println("Drone found more than 3 enemies. Run away?");
                rc.broadcast(Comms.droneRallyPoint, Map.locToInt(myLoc));

            }
            /* else {
				for (RobotInfo target: enemies) {

					for (RobotType struct: structures) {
						if (target.type == struct) {
							rc.broadcast(Comms.droneTarget, Map.locToInt(target.location));
							continue;
						}
					}
				}
			}*/
        }

    }

}
