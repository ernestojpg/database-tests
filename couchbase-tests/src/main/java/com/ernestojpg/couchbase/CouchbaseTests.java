package com.ernestojpg.couchbase;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseAsyncCluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.N1qlMetrics;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.util.retry.RetryBuilder;
import com.ernestojpg.common.BenchmarkUtils;
import com.ernestojpg.common.ParametersExtractor;
import rx.Observable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * CouchbaseTests.java
 *
 * @author Ernesto J. Perez, 2016
 */
public class CouchbaseTests {

	private static final String DEFAULT_HOST = "localhost";
	private static final String DEFAULT_BUCKET = CouchbaseAsyncCluster.DEFAULT_BUCKET;
	private static final int DEFAULT_ITERATIONS = 1_000;
	private static final int DEFAULT_THREADS = 1;
	private static final int DEFAULT_QUERY_ENDPOINTS = 100;

	private CouchbaseTests(ParametersExtractor extractor) {
		final String action = extractor.getAction();
		final int iterations = extractor.getIntegerParameter("iterations", DEFAULT_ITERATIONS);
		final int threads = extractor.getIntegerParameter("threads", DEFAULT_THREADS);
		final int iterationsPerThread = iterations / threads;
		final int queryEndpoints = extractor.getIntegerParameter("queryEndpoints", DEFAULT_QUERY_ENDPOINTS);

		final CouchbaseEnvironment env = DefaultCouchbaseEnvironment
				.builder()
				.queryEndpoints(queryEndpoints)
				.build();
		final Cluster cluster = CouchbaseCluster.create(env, extractor.getStringParameter("host", DEFAULT_HOST));
		final Bucket bucket = cluster.openBucket(extractor.getStringParameter("bucket", DEFAULT_BUCKET),
				extractor.getStringParameter("password", null));

		switch (action.toUpperCase()) {
			case "PREPAREDATA":
				prepareData(bucket, iterationsPerThread);
				break;
			case "TEST1":
				System.out.println("Number of threads: " + threads);
				System.out.println("Number of iterations per thread: " + iterationsPerThread);
				System.out.println("Running Test1 ...");
				runTest1(bucket, iterationsPerThread, threads);
				break;
			case "TEST2":
				System.out.println("Number of threads: " + threads);
				System.out.println("Number of iterations per thread: " + iterationsPerThread);
				System.out.println("Query endpoints: " + queryEndpoints);
				System.out.println("Running Test2 ...");
				runTest2(bucket, iterationsPerThread, threads);
				break;
			default:
				throw new IllegalArgumentException("Unknown action '" + action + "'");
		}
	}

	private void prepareData(Bucket bucket, int numDocuments) {
		System.out.printf("Inserting %,d new documents ...\n", numDocuments);
		Observable
				.range(0, numDocuments)
				.flatMap(n -> {
					final JsonObject user = JsonObject.empty()
							.put("firstname", "Walter " + n)
							.put("lastname", "White")
							.put("job", "chemistry teacher")
							.put("age", 50);
					final JsonDocument doc = JsonDocument.create("walter-" + n, user);
					return bucket.async().upsert(doc)
							.retryWhen(RetryBuilder
									.anyOf(BackpressureException.class)
									.delay(Delay.exponential(TimeUnit.MILLISECONDS, 100))
									.max(10)
									.build());
				})
				.toBlocking()
				.last();

		// Create a N1QL Primary Index (but ignore if it exists)
		bucket.bucketManager().createN1qlPrimaryIndex(true, false);
		System.out.println("Created primary index");
		N1qlQueryResult indexResult = bucket.query(N1qlQuery.simple("CREATE INDEX firstname_index ON default(firstname) USING GSI"));
		System.out.println("Created secondary index: " + indexResult.finalSuccess());
	}

	private void runTest1(Bucket bucket, int iterationsPerThread, int threads) {
		BenchmarkUtils.runParallelAsyncBenchmark(iterationsPerThread, threads, (thread,iteration) -> {
			final CompletableFuture<JsonDocument> future = new CompletableFuture<>();

			final int id = thread * iterationsPerThread + iteration;
			bucket.async().get("walter-" + id)
//					.retryWhen(RetryBuilder
//							.anyOf(BackpressureException.class)
//							.delay(Delay.exponential(TimeUnit.MILLISECONDS, 100))
//							.max(10)
//							.build())
					.doOnError(future::completeExceptionally)
					.forEach(future::complete);

			return future;
		});
	}

	private void runTest2(Bucket bucket, int iterationsPerThread, int threads) {
		BenchmarkUtils.runParallelAsyncBenchmark(iterationsPerThread, threads, (thread,iteration) -> {
			final CompletableFuture<N1qlMetrics> future = new CompletableFuture<>();

			final int id = thread * iterationsPerThread + iteration;
			final N1qlQuery query = N1qlQuery.parameterized("SELECT firstname FROM default WHERE firstname = $1",
					JsonArray.from("Walter " + id),
					N1qlParams.build().adhoc(false));

			bucket.async().query(query)
					.flatMap(AsyncN1qlQueryResult::info)
					.doOnError(future::completeExceptionally)
					.forEach(future::complete);

			return future;
		});
	}

	private static void printOptions() {
		System.out.println("How to use: java -jar couchbase-tests-exec.jar [options] action");
		System.out.println("Where possible options are:");
		System.out.println("  --host=<host>                      (default " + DEFAULT_HOST + ")");
//		System.out.println("  --port=<port>                      (default " + DEFAULT_PORT + ")");
		System.out.println("  --bucket=<bucket>                  (default " + DEFAULT_BUCKET + ")");
		System.out.println("  --password=<password>              (default <no password>)");
		System.out.println("  --iterations=<iterations>          (default " + DEFAULT_ITERATIONS + ")");
		System.out.println("  --threads=<threads>                (default " + DEFAULT_THREADS + ")");
		System.out.println("  --queryEndpoints=<queryEndpoints>  (default " + DEFAULT_QUERY_ENDPOINTS + ")");
		System.out.println("And where possible actions are:");
		System.out.println("  prepareData   (for preparing the data for the tests)");
		System.out.println("  test1         (for executing getByIds test)");
		System.out.println("  test2         (for executing findByFirstname test)");
	}

	public static void main(String[] args) {
		if (args.length==0) {
			printOptions();
			return;
		}
		try {
			final ParametersExtractor extractor = new ParametersExtractor(args);
			new CouchbaseTests(extractor);
		} catch (IllegalArgumentException ex) {
			System.err.println(ex.getMessage());
			printOptions();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
