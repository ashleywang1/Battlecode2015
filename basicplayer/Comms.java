package basicplayer;

public class Comms {
	
	//HQ counts the number of overall allies
	public static final int beaverCount = 1; 
	public static final int soldierCount = 2;
	public static final int basherCount = 3;
	public static final int barracksCount = 4;
	public static final int tankfactoryCount = 104;
	public static final int minerCount = 5;
	public static final int miningfactoryCount = 6;
	public static final int supplydepotCount = 7;
	public static final int MITCount = 8;
	public static final int TrainingFieldCount = 9;
	public static final int commanderCount = 10;
	
	//channel to broadcast which strategy for atking towers
	public static final int towerStrategy = 200;
	
	/*When a beaver is spawned from the HQ, use this channel to communicate info.
	This info is usually read and then rebroadcasted to the robot's own ID channel. */
	public static final int HQtoSpawnedBeaver = 100;
	
	public static final int bestOreFieldLoc = 11;
	public static final int bestOreFieldAmount = 12;
	public static int bestOreFieldDistance = 13;
	
	
	//Army information
	public static final int rushOver = 14;
	public static final int casualties = 15;
	
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
	public static int maxBeavers = 24;
	
	//robot ID channel
	//All 4 digit broadcasting channels are reserved for robot ID channels. (ID - fist digit)
	//A robot can broadcast to and read from the channel corresponding to its ID's first 4 digits. This is used to preserve memory between rounds.

	public static int memory(int id) {
		
		return id%10000;
	}
	
}
