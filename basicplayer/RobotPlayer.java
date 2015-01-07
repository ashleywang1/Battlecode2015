package basicplayer;

import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
	static RobotController rc;
	static Team myTeam;
	static Team enemyTeam;
	
	static int myRange;
	
	static Random rand;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	
	public static void run(RobotController RC) {
		rc = RC;
				
		while(true) {
			try {
				//information
                rc.setIndicatorString(0, "This is an indicator string.");
                rc.setIndicatorString(1, "I am a " + rc.getType());
                
                //MAIN
        		if (rc.getType() == RobotType.HQ) {
        			runHQ(rc);
        		} else if (rc.getType() == RobotType.BEAVER) {
        			runBeaver(rc);
        		}
        		//MINING units
        		else if (rc.getType() == RobotType.MINERFACTORY) {
        			runMinerFactory(rc);
        		} else if (rc.getType() == RobotType.MINER) {
        			runMiner(rc);
        		}
        		//GROUND ARMY units
        		else if (rc.getType() == RobotType.BARRACKS) {
        			runBarracks();
        		} else if (rc.getType() == RobotType.SOLDIER) {
        			runSoldier();
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
            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();

            }

		}
	}

}
