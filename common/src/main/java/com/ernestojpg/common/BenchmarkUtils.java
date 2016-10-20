package com.ernestojpg.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * BenchmarkUtils.java
 *
 * @author Ernesto J. Perez, 2016
 */
public class BenchmarkUtils {

	public static void runParallelBenchmark(int iterationsPerThread, int threads, BiConsumer<Integer,Integer> runnable) {
		final int totalIterations = iterationsPerThread * threads;
		final ExecutorService pool = Executors.newFixedThreadPool(threads);

		final CompletableFuture[] futures = new CompletableFuture[threads];
		final long[] execTimes = new long[totalIterations];

		final long initTime = System.currentTimeMillis();
		for (int t=0 ; t<threads ; t++) {
			int thread = t;
			futures[t] = CompletableFuture.runAsync(() -> {
				for (int n = 0; n < iterationsPerThread; n++) {
					final long time = System.currentTimeMillis();
					runnable.accept(thread, n);
					final long execTime = System.currentTimeMillis() - time;
					//					System.out.printf("I[%,d]T[%s]: %,d millis executing query.\n",
					//							n, Thread.currentThread().getName(), execTime);
					execTimes[thread * iterationsPerThread + n] = execTime;
				}
			}, pool);
		}

		CompletableFuture.allOf(futures).join();

		final long totalTime = System.currentTimeMillis() - initTime;
		System.out.printf("Iterations: %,d, Total time: %,d millis (%,d/second)\n", totalIterations,
				totalTime, (totalIterations * 1000) / totalTime);
		printStatistics(execTimes);
	}

	public static void runParallelAsyncBenchmark(int iterationsPerParallelismUnit, int parallelismUnits, BiFunction<Integer,Integer,CompletableFuture<?>> asyncFunc) {
		final int totalIterations = iterationsPerParallelismUnit * parallelismUnits;
		final CompletableFuture[] futures = new CompletableFuture[parallelismUnits];
		//final List<CompletableFuture<Void>> futures = new ArrayList<>();
		final long[] execTimes = new long[totalIterations];

		final long initTime = System.currentTimeMillis();
		for (int t=0 ; t<parallelismUnits ; t++) {
			CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
			for (int n = 0; n < iterationsPerParallelismUnit; n++) {
				final IterationData data = new IterationData(t, n);
				future = future.thenCompose(v -> {
					data.time = System.currentTimeMillis();
					return asyncFunc.apply(data.thread, data.iteration);
				}).thenAccept(result -> {
					final long execTime = System.currentTimeMillis() - data.time;
					//System.out.printf("I[%,d]T[%s]: %s, %,d millis executing query.\n", data.iteration, data.thread, result, execTime);
					execTimes[data.thread * iterationsPerParallelismUnit + data.iteration] = execTime;
				});
			}
			futures[t] = future;
		}

		CompletableFuture.allOf(futures).join();

		final long totalTime = System.currentTimeMillis() - initTime;
		System.out.printf("Iterations: %,d, Total time: %,d millis (%,d/second)\n", totalIterations,
				totalTime, (totalIterations * 1000) / totalTime);
		printStatistics(execTimes);
	}

	private static void printStatistics(final long[] execTimes) {
		long max = -1;
		long min = Long.MAX_VALUE;
		long sum = 0;

		for (int i=0 ; i<execTimes.length ; i++) {
			final long value = execTimes[i];
			sum += value;
			if (value > max) max = value;
			if (value < min) min = value;
		}
		final double avg = ((double)sum) / execTimes.length;
		System.out.printf("Max: %,d millis, Min: %,d millis, Average: %.0f millis\n", max, min, avg);
	}

	private static class IterationData {
		final Integer thread;
		final Integer iteration;
		Long time;
		Object asyncResult;

		IterationData(Integer thread, Integer iteration) {
			this.thread = thread;
			this.iteration = iteration;
		}
	}

}
