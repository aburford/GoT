import java.util.Random;
import java.util.ArrayList;

public class Player {

	// keep reference to simulation to get time step
	GoTSimulation simulation;

	// used to estimate expectations for each arm:
	double[] rewards; // sum of rewards from each arm
	int[] playCounts; // count of non collision reward from each arm

	boolean content;
	int baseline; // index of arm to play for baseline action
	int armCount;
	double epsilon = 0.5; // TODO no clue what this should be set to
	double c = 5; // TODO I have even less of a clue how we find this value
					// it shouldn't be possible without more information
	double maxRewardEstimate; // an estimate of the maximum reward we can receive across all strategy profiles
	int[] cumulativeContentCounter; // contains content counter for previous k/2 epochs
	int exploitArm; // index of arm to exploit

	ArrayList<Integer> lastActions = new ArrayList<Integer>(); // index of the
																// last arm
																// played in the
																// ith epoch

	int[] contentCounter; // count the number of times each arm has resulted in
							// content for current epoch
	// this could be a queue to save half the memory
	ArrayList<int[]> counters = new ArrayList<int[]>(); // content counter for
														// ith epoch
	Random rng;

	public Player(GoTSimulation simulation, int armCount, Random rng) {
		this.simulation = simulation;
		this.rng = rng;
		rewards = new double[armCount];
		playCounts = new int[armCount];
		cumulativeContentCounter = new int[armCount];
		contentCounter = new int[armCount];
		this.armCount = armCount;
	}

	// Winter is coming, prepare for the Game Of Thrones phase!
	// k is the epoch index
	public void winterIsComing(int k) {
		for (int i = 0; i < contentCounter.length; i++)
			contentCounter[i] = 0;
		if (k < 2)
			baseline = randomArm(false);
		else
			baseline = lastActions.get(k - k / 2 - 1);
		content = true;
		maxRewardEstimate = 0;
		for (int i = 0; i < armCount; i++) {
			if (playCounts[i] == 0)
				continue;
			double rewardEstimate = rewards[i] / playCounts[i];
//			System.out.printf("%.0f ", rewardEstimate);
			if (rewardEstimate > maxRewardEstimate)
				maxRewardEstimate = rewardEstimate;
		}
//		System.out.println();
	}

	// noBaseline specifies whether the random arm can be equal to the baseline
	private int randomArm(boolean noBaseline) {
		if (noBaseline) {
			int arm = Math.abs(rng.nextInt()) % (armCount - 1);
			if (arm >= baseline)
				return arm + 1;
		}
		return Math.abs(rng.nextInt()) % armCount;
	}
	
	// k is the index of the current epoch
	public void prepareForExploitation(int k) {
		// end GoT phase
		counters.add(contentCounter);
		// update cumulativeContentCounter to only contain previous k/2 epoch counts
		for (int i = 0; i < armCount; i++) {
			cumulativeContentCounter[i] += contentCounter[i];
			if (k % 2 == 1 && k > 1)	
				cumulativeContentCounter[i] -= counters.get(k / 2 - 1)[i];
		}
		lastActions.add(baseline);

		// decide which arm to exploit
		// we will choose the arm with the highest total contentCounter over the previous k/2 epochs
		int[] max = {0, cumulativeContentCounter[0]};
		for (int i = 1; i < armCount; i++)
			if (cumulativeContentCounter[i] > max[1]) {
				max[0] = i;
				max[1] = cumulativeContentCounter[i];
			}
		exploitArm = max[0];
	}

	// should only be called after reward is received
	public int chooseArm(Phase phase) {
		if (phase == Phase.EXPLORE)
			return randomArm(false);
		if (phase == Phase.GOT) {
			if (content) {
				double baselineCutoff = Math.pow(epsilon, c);
//				System.out.println("baseline cutoff: " + baselineCutoff);
				double random = rng.nextDouble();
				if (random < baselineCutoff)
					return baseline;
				else // choose random arm that's not the baseline
					return randomArm(true);
			} else {
				// discontent players choose an arm at random
				return randomArm(false);
			}
		}
		// EXPLOIT phase
		return exploitArm;
	}

	// should be called after chooseArm() is called
	public void receiveReward(int arm, double reward, Phase phase) {
		if (phase == Phase.EXPLORE) {
			// update expectations
			rewards[arm] += reward;
			if (reward != 0)
				playCounts[arm]++;
		}
		if (phase == Phase.GOT) {
			// perform state transition
			// this means possible updating baseline and contentedness
			if (arm != baseline || reward == 0 || content == false) {
				baseline = arm;
				// see (9) on page 6
//				System.out.printf("content vars: %f %f %f\n", reward, maxRewardEstimate, epsilon);
//				System.out.println("Content cutoff:" + reward / maxRewardEstimate * Math.pow(epsilon, maxRewardEstimate - reward));
//				System.out.println("Content exp:" + Math.pow(epsilon, maxRewardEstimate - reward));
				// we could scale exponent of epsilon so it's not so extreme
				content = rng.nextDouble() < reward / maxRewardEstimate * Math.pow(epsilon, (maxRewardEstimate - reward));
			}
			// update the contentCounter
			if (content)
				contentCounter[arm]++;
		}
		// else we're exploiting in which case we don't care about reward
	}

}
