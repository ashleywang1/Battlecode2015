package basicplayer;

public class Comms {
	
	//HQ counts the number of overall allies
	public static final int beaverCount = 1; 
	public static final int soldierCount = 2;
	public static final int basherCount = 3;
	public static final int barracksCount = 4;
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
	
	
	//Army information
	public static final int rushOver = 13;
	public static final int casualties = 14;
	
	//robot ID channel
	//All 4 digit broadcasting channels are reserved for robot ID channels. (ID - fist digit)
	//A robot can broadcast to and read from the channel corresponding to its ID's first 4 digits. This is used to preserve memory between rounds.

	public static int memory(int id) {
		
		return id%10000;
	}
	
}
