package ashleyplayer;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {
	static RobotController rc;
	static Team myTeam;
	static Team enemyTeam;
	static int myRange;
	static Random rand;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	public static void run(RobotController tomatojuice) {
		rc = tomatojuice;
        rand = new Random(rc.getID());

		myRange = rc.getType().attackRadiusSquared;
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
        Direction lastDirection = null;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		RobotInfo[] myRobots;
		
		MapLocation[] myTowers;
		
		//run only once, when just spawned
		try {
			if (rc.getType() == RobotType.HQ) {
				//find best location to put miningfactories
				List<MapLocation> corners = new ArrayList<MapLocation>();
				double ore;
				
				if (rc.canSenseLocation(rc.getLocation().add(5,5))) {
					ore = rc.senseOre(rc.getLocation().add(5, 5));
					corners.add(rc.getLocation().add(5, 5));
				}
				if (rc.canSenseLocation(rc.getLocation().add(5,-5))) {
					ore = rc.senseOre(rc.getLocation().add(5, -5));
					corners.add(rc.getLocation().add(5, -5));
				}
				if (rc.canSenseLocation(rc.getLocation().add(-5,5))) {
					ore = rc.senseOre(rc.getLocation().add(-5, 5));
					corners.add(rc.getLocation().add(-5, 5));
				}
				if (rc.canSenseLocation(rc.getLocation().add(-5,-5))) {
					ore = rc.senseOre(rc.getLocation().add(-5, -5));
					corners.add(rc.getLocation().add(-5, -5));
				}
				
				System.out.println(corners);
				System.out.println("corners!");
				//place barracks near towers, in order of closest to farthest
				
			}
			else if (rc.getType() == RobotType.BEAVER) {
				int HQAssignment = rc.readBroadcast(Comms.HQtoSpawnedBeaver);
				rc.broadcast((rc.getID()%10000), HQAssignment);			
			}
			
		} catch (GameActionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("Initialization Exception for robot:" + rc.getID());
		}

		while(true) {
            try {
                rc.setIndicatorString(0, "This is an indicator string.");
                rc.setIndicatorString(1, "I am a " + rc.getType());
            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();

            }

			if (rc.getType() == RobotType.HQ) {
				try {					
					int fate = rand.nextInt(10000);
					myRobots = rc.senseNearbyRobots(999999, myTeam);
					int numSoldiers = 0;
					int numBashers = 0;
					int numBeavers = 0;
					int numBarracks = 0;
					int numMinerFactories = 0;
					
					boolean spawnSuccess = false;
					
					for (RobotInfo r : myRobots) {
						RobotType type = r.type;
						if (type == RobotType.SOLDIER) {
							numSoldiers++;
						} else if (type == RobotType.BASHER) {
							numBashers++;
						} else if (type == RobotType.BEAVER) {
							numBeavers++;
						} else if (type == RobotType.BARRACKS) {
							numBarracks++;
						} else if (type == RobotType.MINERFACTORY) {
							numMinerFactories++;
						} 
					}
					rc.broadcast(0, numBeavers);
					rc.broadcast(1, numSoldiers);
					rc.broadcast(2, numBashers);
					rc.broadcast(100, numBarracks);
					
					if (rc.isWeaponReady()) {
						attackSomething();
					}

					if (rc.isCoreReady() && rc.getTeamOre() >= 100 && fate < Math.pow(1.2,12-numBeavers)*10000) {
						spawnSuccess = trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
					}
					
					if (spawnSuccess) {
						rc.broadcast(Comms.HQtoSpawnedBeaver, 0);
					}
				} catch (Exception e) {
					System.out.println("HQ Exception");
                    e.printStackTrace();
				}
			}
			
            if (rc.getType() == RobotType.TOWER) {
                try {					
					if (rc.isWeaponReady()) {
						attackSomething();
					}
				} catch (Exception e) {
					System.out.println("Tower Exception");
                    e.printStackTrace();
				}
					else{
						buildUnit(RobotType.BARRACKS);
            }
			
			
			if (rc.getType() == RobotType.BASHER) {
                try {
                    RobotInfo[] adjacentEnemies = rc.senseNearbyRobots(2, enemyTeam);

                    // BASHERs attack automatically, so let's just move around mostly randomly
					if (rc.isCoreReady()) {
						int fate = rand.nextInt(1000);
						if (fate < 800) {
							tryMove(directions[rand.nextInt(8)]);
						} else {
							tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
						}
					}
                } catch (Exception e) {
					System.out.println("Basher Exception");
					e.printStackTrace();
                }
            }
			
            if (rc.getType() == RobotType.SOLDIER) {
                try {
                    if (rc.isWeaponReady()) {
						attackSomething();
					}
					if (rc.isCoreReady()) {
						int fate = rand.nextInt(1000);
						if (fate < 800) {
							tryMove(directions[rand.nextInt(8)]);
						} else {
							tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
						}
					}
                } catch (Exception e) {
					System.out.println("Soldier Exception");
					e.printStackTrace();
                }
            }
			
			if (rc.getType() == RobotType.BEAVER) {
				try {
					//attacking
					if (rc.isWeaponReady()) {
						attackSomething();
					}
					
					if (rc.isCoreReady()) {
						/*
						//get information
						int numBeavers = rc.readBroadcast(0);
						int numMinerFactories = rc.readBroadcast(4);
						
						//explore
						if (numBeavers < 20) {
							tryMove(directions[rand.nextInt(8)]);
						}
						
						double ore = rc.senseOre(rc.getLocation());
						
						if (rc.getTeamOre() > 500 && numMinerFactories < 2 && ore >15) {
							tryBuild(directions[rand.nextInt(8)],RobotType.MINERFACTORY);
						}
						*/
						//
						int fate = rand.nextInt(1000);
						if (fate < 8 && rc.getTeamOre() >= 300) {
							tryBuild(directions[rand.nextInt(8)],RobotType.BARRACKS);
						} else if (fate < 600) {
							rc.mine();
						} else if (fate < 900) {
							tryMove(directions[rand.nextInt(8)]);
						} else {
							tryMove(rc.senseHQLocation().directionTo(rc.getLocation()));
						}
					}
				} catch (Exception e) {
					System.out.println("Beaver Exception");
                    e.printStackTrace();
				}
			}

            if (rc.getType() == RobotType.BARRACKS) {
				try {
					int fate = rand.nextInt(10000);
					
                    // get information broadcasted by the HQ
					int numBeavers = rc.readBroadcast(0);
					int numSoldiers = rc.readBroadcast(1);
					int numBashers = rc.readBroadcast(2);
					
					if (rc.isCoreReady() && rc.getTeamOre() >= 60 && fate < Math.pow(1.2,15-numSoldiers-numBashers+numBeavers)*10000) {
						if (rc.getTeamOre() > 80 && fate % 2 == 0) {
							trySpawn(directions[rand.nextInt(8)],RobotType.BASHER);
						} else {
							trySpawn(directions[rand.nextInt(8)],RobotType.SOLDIER);
						}
					}
				} catch (Exception e) {
					System.out.println("Barracks Exception");
                    e.printStackTrace();
				}
			}
			
			rc.yield();
		}
	}
	
    // This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
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
		
	}

	private static void buildUnit(RobotType type) throws GameActionException {
		if(rc.getTeamOre()>RobotType.MINERFACTORY.oreCost){
			Direction buildDir = getRandomDirection();
			if(rc.isCoreReady()&&rc.canBuild(buildDir, type));
				rc.build(buildDir, type);
		}
		
	}

	private static void attackEnemyZero() throws GameActionException {
		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		//shoot at any enemy (can choose which type later, lowest hp etc)
		if(nearbyEnemies.length>0){ //there exists enemies near
			//try to shoot at enemy specified by nearbyenemies[0]
			if(rc.isWeaponReady()&&rc.canAttackLocation(nearbyEnemies[0].location)){
				rc.attackLocation(nearbyEnemies[0].location);
			}
		}
	}

	private static void spawnUnit(RobotType type) throws GameActionException {
		Direction randomDir = getRandomDirection();
		if(rc.isCoreReady()&&rc.canSpawn(randomDir,type)){
			rc.spawn(randomDir, type);
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
			else{
				facing = facing.rotateRight();
			}
	}
		MapLocation tileInFront = rc.getLocation().add(facing);
		
		//check tile in front cannot be atked by enemy towers
		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		boolean tileInFrontSafe = true;
		for(MapLocation m: enemyTowers){
			if(m.distanceSquaredTo(tileInFront)<=RobotType.TOWER.attackRadiusSquared){
				tileInFrontSafe = false;
				break;
			}
		}
		//check not facing edge of map or tile in front not safe, turn
		if(rc.senseTerrainTile(tileInFront)!=TerrainTile.NORMAL||!tileInFrontSafe){
			facing = facing.rotateLeft();
		}else{
			if(rc.isCoreReady()&&rc.canMove(facing)){
				rc.move(facing);
			}
		}
	}

	
}
