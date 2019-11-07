import java.util.Arrays;
import java.util.Random;
public class Arm {
	
	// we'll use a normal distribution cause that satisfies Bernstein's condition which I don't understand
	double means[], stddevs[];
	
	Random rng;
	
	public Arm(int playerCount, Random rng) {
		this.rng = rng;
		means = new double[playerCount];
		stddevs = new double[playerCount];
		for (int i = 0; i < playerCount; i++) {
			// we'll make means integers between 20 and 100 inclusive
			means[i] = Math.abs(rng.nextInt()) % 81 + 20;
			// we'll make standard deviations uniform between 5 and 10
			stddevs[i] = rng.nextDouble() * 5 + 5;
		}
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(String.format("Arm with means: %s\n  and std devs:  ", Arrays.toString(means)));
		for (double stddev : stddevs)
			str.append(String.format("%.2f  ", stddev));
		return str.toString();
	}
	
	public double getReward(int playerIndex) {
		double reward = rng.nextGaussian() * stddevs[playerIndex] + means[playerIndex];
		// technically no longer normal but this is unlikely I think we're fine
		if (reward < 0)
			reward = -reward;
		// in theory this would be infinitely unlikely, i guess we'll just give the mean
		else if (reward == 0)
			reward = means[playerIndex];
		return reward;
	}
}
