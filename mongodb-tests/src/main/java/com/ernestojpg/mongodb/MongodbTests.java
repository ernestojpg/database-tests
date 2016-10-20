package com.ernestojpg.mongodb;

import com.ernestojpg.common.BenchmarkUtils;
import com.ernestojpg.common.ParametersExtractor;
import com.mongodb.ServerAddress;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.connection.ClusterSettings;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * MongodbTests.java
 *
 * @author Ernesto J. Perez, 2016
 */
public class MongodbTests {

	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 27017;
	private static final String DEFAULT_DATABASE = "testdb";
	private static final int DEFAULT_ITERATIONS = 1_000;
	private static final int DEFAULT_THREADS = 1;

	private MongodbTests(ParametersExtractor extractor) {
		final String action = extractor.getAction();
		final String host = extractor.getStringParameter("host", DEFAULT_HOST);
		final int port = extractor.getIntegerParameter("port", DEFAULT_PORT);
		final String databaseName = extractor.getStringParameter("database", DEFAULT_DATABASE);
		final int iterations = extractor.getIntegerParameter("iterations", DEFAULT_ITERATIONS);
		final int threads = extractor.getIntegerParameter("threads", DEFAULT_THREADS);
		final int iterationsPerThread = iterations / threads;

//		ConnectionPoolSettings connectionSettings = ConnectionPoolSettings.builder()
//				.maxSize(50).build();

		final ClusterSettings clusterSettings = ClusterSettings.builder()
				.hosts(Collections.singletonList(new ServerAddress(host, port)))
				.build();
		final MongoClientSettings settings = MongoClientSettings.builder()
				.clusterSettings(clusterSettings)
//				.connectionPoolSettings(connectionSettings)
				.build();
		final MongoClient mongoClient = MongoClients.create(settings);


		final MongoDatabase mongodb = mongoClient.getDatabase(databaseName);
		final MongoCollection<Document> collection = mongodb.getCollection("test-collection");

		switch (action.toUpperCase()) {
			case "PREPAREDATA":
				prepareData(collection, iterationsPerThread);
				break;
			case "TEST1":
				System.out.println("Number of threads: " + threads);
				System.out.println("Number of iterations per thread: " + iterationsPerThread);
				System.out.println("Database: " + databaseName);
				System.out.println("Running Test1 ...");
				runTest1(collection, iterationsPerThread, threads);
				break;
			case "TEST2":
				System.out.println("Number of threads: " + threads);
				System.out.println("Number of iterations per thread: " + iterationsPerThread);
				System.out.println("Database: " + databaseName);
				System.out.println("Running Test2 ...");
				runTest2(collection, iterationsPerThread, threads);
				break;
			default:
				throw new IllegalArgumentException("Unknown action '" + action + "'");
		}

	}

	private void prepareData(MongoCollection<Document> collection, int numDocuments) {
		System.out.printf("Inserting %,d new documents ...\n", numDocuments);
		final List<Document> list = new ArrayList<>(numDocuments);
		for (int n=0 ; n< numDocuments ; n++) {
			final Document document = new Document()
					.append("_id", "walter-" + n)
					.append("firstname", "Walter " + n)
					.append("lastname", "White")
					.append("job", "chemistry teacher")
					.append("age", 50);
			list.add(document);
		}

		final CountDownLatch latch = new CountDownLatch(1);
		collection.insertMany(list, (v,th) -> {
			if (th!=null) {
				th.printStackTrace();
				latch.countDown();
				return;
			}

			// Create a UNIQUE ASCENDING index on the 'firstname' field
			collection.createIndex(new Document("firstname",1), new IndexOptions().unique(true), (result,ex) -> {
				if (ex!=null) {
					ex.printStackTrace();
				} else {
					System.out.printf("Created index: %s\n", result);
				}
				latch.countDown();
			});
		});

		try {
			// Wait for all asynchronous operations to finish
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void runTest1(final MongoCollection<Document> collection, final int iterationsPerThread, final int threads) {
		BenchmarkUtils.runParallelAsyncBenchmark(iterationsPerThread, threads, (thread, iteration) -> {
			final CompletableFuture<Document> future = new CompletableFuture<>();
			final long id = thread * iterationsPerThread + iteration;
			collection.find(Filters.eq("_id", "walter-" + id)).first((myDoc, th) -> {
				if (th==null) {
					future.complete(myDoc);
				} else {
					future.completeExceptionally(th);
				}
			});
			return future;
		});
	}

	private void runTest2(final MongoCollection<Document> collection, final int iterationsPerThread, final int threads) {
		BenchmarkUtils.runParallelAsyncBenchmark(iterationsPerThread, threads, (thread,iteration) -> {
			final CompletableFuture<Document> future = new CompletableFuture<>();
			final long id = thread * iterationsPerThread + iteration;
			collection.find(Filters.eq("firstname", "Walter " + id)).first((myDoc, th) -> {
				if (th==null) {
					future.complete(myDoc);
				} else {
					future.completeExceptionally(th);
				}
			});
			return future;
		});
	}

	private static void printOptions() {
		System.out.println("How to use: java -jar mongodb-tests-exec.jar [options] action");
		System.out.println("Where possible options are:");
		System.out.println("  --host=<host>                      (default " + DEFAULT_HOST + ")");
		System.out.println("  --port=<port>                      (default " + DEFAULT_PORT + ")");
		System.out.println("  --database=<database>              (default " + DEFAULT_DATABASE + ")");
		System.out.println("  --password=<password>              (default <no password>)");
		System.out.println("  --iterations=<iterations>          (default " + DEFAULT_ITERATIONS + ")");
		System.out.println("  --threads=<threads>                (default " + DEFAULT_THREADS + ")");
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
			new MongodbTests(extractor);
		} catch (IllegalArgumentException ex) {
			System.err.println(ex.getMessage());
			printOptions();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
