import java.util.Arrays;
import java.util.Random;

public class GoTSimulation {
	
	public static void main(String[] args) {
		GoTSimulation simulation = new GoTSimulation(5, 5000, 10);
		while (simulation.nextKEpoch());
		// the simulation has now reached the time horizon
	}
	
	final int playerCount, timeHorizon, armCount;
//	turn index;
	private int t = 1;
	private int k = 1;
	double kDeltaPower = 0.1;
	Phase phase = Phase.EXPLORE;
	// not sure what are good values for these constants
	int c1 = 5;
	int c2 = 5;
	int c3 = 5;
	
	Random rng = new Random();
	
	Player[] players;
	Arm[] arms;
	
	double optimalReward = 0;
	double totalRegret = 0;
	
	int[] optimal;
	
	public GoTSimulation(int playerCount, int timeHorizon, int armCount) {
		this.playerCount = playerCount;
		this.timeHorizon = timeHorizon;
		this.armCount = armCount;
		if (armCount < playerCount) {
			System.out.println("Cannot have more players than arms");
			System.exit(0);
		}
		arms = new Arm[armCount];
		for (int i = 0; i < armCount; i++) {
			arms[i] = new Arm(playerCount, rng);
		}
		players = new Player[playerCount];
		for (int i = 0; i < playerCount; i++) {
			players[i] = new Player(this, arms.length, rng);
		}
		System.out.println("arms:\n" + arrString(arms));
		// calculate optimal solution so we can calculate total regret
		// we need a cost matrix where rows are players and columns are arms
		double[][] costMatrix = new double[playerCount][armCount];
		for (int r = 0; r < playerCount; r++)
			for (int c = 0; c < armCount; c++)
				// HungarianAlgorithm will minimize objective, so make rewards negative
				costMatrix[r][c] = -arms[c].means[r];
		System.out.println("Cost matrix for hungarian algorithm:");
		for (double[] playerRow : costMatrix) {
			for (double reward : playerRow)
				System.out.print(reward + " ");
			System.out.println();
		}
		HungarianAlgorithm solver = new HungarianAlgorithm(costMatrix);
		// optimal contains the optimal arm index for each player
		optimal = solver.execute();
		System.out.println("optimal solution: " + Arrays.toString(optimal));
		// find the optimal reward for each time step
		for (int i = 0; i < optimal.length; i++)
			optimalReward += arms[optimal[i]].means[i];
		System.out.println("Optimal Reward: " + optimalReward + "\n");
		// only used in the nextTimeStep function
		playerChoices = new int[playerCount];
		collisions = new int[armCount];
	}
	
	public String arrString(Object[] arr) {
		StringBuilder str = new StringBuilder();
		for (Object o : arr)
			str.append(o + "\n");
		return str.toString();
	}
	
	public int getTimeStep() {
		return t;
	}
	
	// returns whether or not we can do another time step
	public boolean nextKEpoch() {
//		System.out.printf("Starting %dth k epoch\n\n", k);
		// explore phase
		if (!runPhase(c1 * Math.pow(k, kDeltaPower), Phase.EXPLORE)) return false;
		// GoT phase
		for (Player p : players)
			p.winterIsComing(k);
		if (!runPhase(c2 * Math.pow(k, kDeltaPower), Phase.GOT)) return false;
		// exploit phase
		int[] playerChoices = new int[players.length]; // for debugging purposes only
		int i = 0;
		for (Player p : players) {
			p.prepareForExploitation(k);
			playerChoices[i++] = p.exploitArm;
		}
		System.out.printf("%dth epoch:\n%s\n", k, Arrays.toString(playerChoices));
		System.out.println(Arrays.toString(optimal));
//		if (!runPhase(c3 * Math.pow(2, k), Phase.EXPLOIT)) return false;
		k++;
		return t != timeHorizon;
	}
	
	public boolean runPhase(double steps, Phase phase) {
//		System.out.printf("Running %s phase for %.0f steps\n\n", phase.toString(), steps);
		for (int step = 0; step < steps; step++)
			if (!nextTimeStep(phase)) return false;
		return true;
	}
	
	// declared outside function for efficiency (does this make a significant difference?)
	private int[] playerChoices;
	private int[] collisions;
	// returns whether or not we can do another time step
	public boolean nextTimeStep(Phase phase) {
//		System.out.printf("Starting time step %d in phase %s of %dth k epoch\n", t, phase.toString(), k);
		for (int i = 0; i < playerCount; i++) {
			playerChoices[i] = players[i].chooseArm(phase);
		}
		// reset collision counts
		for (int i = 0; i < armCount; i++) {
			collisions[i] = 0;
		}
		// count collisions
		for (int choice : playerChoices) {
			collisions[choice]++;
		}
//		System.out.println("Collision counts: " + Arrays.toString(collisions));
//		System.out.println("Choices: " + Arrays.toString(playerChoices));
//		System.out.println("Optimal: " + Arrays.toString(optimal));
		// give rewards back to players
		int choice;
		double totalReward = 0;
		double reward;
		for (int i = 0; i < playerCount; i++) {
			choice = playerChoices[i];
			if (collisions[choice] > 1)
				players[i].receiveReward(choice, 0, phase);
			else {
				reward = arms[choice].getReward(i);
				totalReward += reward;
				players[i].receiveReward(choice, reward, phase);
			}
		}
		// update total regret with regret for this time step
		// TODO instead of optimal reward should I sample from the distribution
		totalRegret += optimalReward - totalReward;
//		System.out.printf("Reward for this time step: %.2f\n", totalReward);
//		System.out.printf("Total regret: %.2f\n\n", totalRegret);
		t++;
		return t != timeHorizon;
	}

}
