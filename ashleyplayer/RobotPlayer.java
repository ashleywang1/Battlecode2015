package ashleyplayer;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {
	
	static Direction facing;
	static Random rand;
	static RobotController rc;
	public static void run(RobotController myrc){
		rc = myrc;
		rand = new Random(rc.getID());
		facing=getRandomDirection(); //randomize starting direction
		while(true){
			try{
				if(rc.getType()==RobotType.HQ){
							attackEnemyZero();
					spawnUnit(RobotType.BEAVER);
				}else if(rc.getType()==RobotType.BEAVER){
					attackEnemyZero();
					if(Clock.getRoundNum()<700){
					buildUnit(RobotType.MINERFACTORY);
					}
					if(Clock.getRoundNum()>1800){
						buildUnit(RobotType.HANDWASHSTATION);
					}
					else{
						buildUnit(RobotType.BARRACKS);
					}
					mineAndMove();
				
				}else if(rc.getType()==RobotType.MINER){
					attackEnemyZero();
					mineAndMove();
				}else if(rc.getType()==RobotType.BARRACKS){
					spawnUnit(RobotType.SOLDIER);
				}else if(rc.getType()==RobotType.MINERFACTORY){
					spawnUnit(RobotType.MINER);
				}else if(rc.getType()==RobotType.TOWER){
					attackEnemyZero();
				}else if(rc.getType()==RobotType.SOLDIER){
					attackEnemyZero();
					moveAround();
				}
				transferSupplies();
			
				
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			rc.yield();
		}
	}

	private static void transferSupplies() throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(),GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
		double lowestSupply = rc.getSupplyLevel();
		double transferAmount = 0;
		MapLocation suppliesToThisLoc = null;
		for (RobotInfo ri:nearbyAllies){
			if(ri.supplyLevel<lowestSupply){
				lowestSupply = ri.supplyLevel;
				transferAmount = (rc.getSupplyLevel()-ri.supplyLevel)/2;
				suppliesToThisLoc = ri.location;
				
			}
			if(suppliesToThisLoc!=null){
				rc.transferSupplies((int)transferAmount, suppliesToThisLoc);
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

	private static Direction getRandomDirection() {

		return Direction.values()[(int)(rand.nextDouble()*8)];
	}

	private static void mineAndMove() throws GameActionException {
		if(rc.senseOre(rc.getLocation())>10){
			if(rc.isCoreReady()&&rc.canMine()){
			rc.mine();
			}
		}
		else{ //no ore, move around
			moveAround();
		}
		
	}

	private static void moveAround() throws GameActionException {
		if(rand.nextDouble()<0.05){
			if(rand.nextDouble()<.5){
				facing = facing.rotateLeft();
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
