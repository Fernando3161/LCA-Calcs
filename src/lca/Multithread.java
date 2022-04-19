package lca;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class PiMonteCarlo {
	AtomicInteger nAtomSuccess;
	int nThrows;
	double value;
	class MonteCarlo implements Runnable {
		@Override
		public void run() {
			double x = Math.random();  // here goes the proper function
			double y = Math.random();
			if (x * x + y * y <= 1)
				nAtomSuccess.incrementAndGet();
		}
	}
	public PiMonteCarlo(int i) {
		this.nAtomSuccess = new AtomicInteger(0);
		this.nThrows = i;
		this.value = 0;
	}
	public double getPi() {
		int nProcessors = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newWorkStealingPool(nProcessors);
		for (int i = 1; i <= nThrows; i++) {
			Runnable worker = new MonteCarlo();
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		value = 4.0 * nAtomSuccess.get() / nThrows;
		return value;
	}
}


public class Multithread {
	public static void main(String[] args) {
		PiMonteCarlo PiVal = new PiMonteCarlo(100000000);
		long startTime = System.currentTimeMillis();
		double value = PiVal.getPi();
		long stopTime = System.currentTimeMillis();
		System.out.println("Approx value:" + value);
		System.out.println("Difference to exact value of pi: " + (value - Math.PI));
		System.out.println("Error: " + (value - Math.PI) / Math.PI * 100 + " %");
		System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
		System.out.println("Time Duration: " + (stopTime - startTime) + "ms");
	}
}
