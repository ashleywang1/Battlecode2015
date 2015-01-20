package huntingplayer;

public class Comms {
	
	//HQ counts the number of overall allies
	public static final int beaverCount = 1; 
	public static final int soldierCount = 2;
	public static final int basherCount = 3;
	public static final int barracksCount = 4;
	public static final int tankfactoryCount = 104;
	public static final int tanksCount = 105;
	public static final int helipadCount = 106;
	public static final int droneCount = 107;
	public static final int aerospacelabCount = 108;
	public static final int launcherCount = 109;
	public static final int minerCount = 5;
	public static final int miningfactoryCount = 6;
	public static final int supplydepotCount = 7;
	public static final int MITCount = 8;
	public static final int TrainingFieldCount = 9;
	public static final int commanderCount = 10;
	

	
	/*When a beaver is spawned from the HQ, use this channel to communicate info.
	This info is usually read and then rebroadcasted to the robot's own ID channel. */
	public static final int HQtoSpawnedBeaver = 100;
	
	//Miners use this
	public static final int bestOreFieldLoc = 11;
	public static final int bestOreFieldAmount = 12;
	public static int bestOreFieldDistance = 13;
	public static int enemiesNearMiners = 14;
	
	
	//Army information
	public static final int rushOver = 300;
	public static final int rushStartRound = 301;
	public static final int casualties = 302;
	
	//Drone information
	public static final int droneRallyPoint = 303;
	public static final int droneTarget = 304;
	//public static final int droneRotation = 305;
	
	//Supply information
	public static final int lowestBarracksSupply = 16;
	public static final int lowestBarracksSupplyLoc = 17;
	public static final int lowestMiningFactorySupply = 18;
	public static final int lowestMiningFactorySupplyLoc = 19;
	public static final int lowestSoldierSupply = 20;
	public static final int lowestSoldierSupplyLoc = 21;
	public static final int lowestMinerSupply = 22;
	public static final int lowestMinerSupplyLoc = 23;
	
	
	//strategy
	public static final int strategy = 200; //channel to broadcast which strategy for attacking towers
	public static int maxBeavers = 24;
	public static int spawnBeaver = 25;
	public static int towerDistressCall = 26;
	public static int defensiveRally = 27;
	
	//robot ID channel
	//All 4 digit broadcasting channels are reserved for robot ID channels. (ID - fist digit)
	//A robot can broadcast to and read from the channel corresponding to its ID's first 4 digits. This is used to preserve memory between rounds.
	public static int memory(int id) {
		int memory = id%10000;
		if (memory < 100) {
			memory = memory*100;
		} else if (memory < 1000) {
			memory = memory*10;
		}
		if (memory < 1000) {
			System.out.println("THIS WAS NEVER SUPPOSED TO HAPPEN");
		}
		return memory;
	}
	
}
