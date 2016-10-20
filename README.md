
# Database Tests

In this project we will perform several tests/benchmarks to different databases, mainly NoSQL databases.

## Compile project

You will need JDK 8, and Maven 3.x. For compiling the project just enter:

```
mvn clean package
```

## Tests

Firstly, for all tests we will insert in the Database a given number `N` of JSON documents with this structure:

```json
// ID: walter-<num>
{
  "firstname": "Walter <num>",
  "lastname": "White",
  "job": "chemistry teacher",
  "age": 50
}
```

Where `<num>` takes values from `0` to `N-1`.

In all cases, we will perform two main tests:
* Get all documents one by one, by their ID
* Search all documents one by one, by their firstname.

In the examples below we assume you are running the Database servers in your local machine. If the servers are running in a different machine you will have to indicate the corresponding hostname or IP with the parameter `--host=<host>`

## Couchbase Tests

### Prepare data

Firstly, we will insert 200K documents in the database, entering this command:

```
java -jar couchbase-tests/target/couchbase-tests-exec.jar --iterations=200000 --host=localhost prepareData
```

By default it will create the documents in the `default` bucket.

### Run Test1

Simple `get()` by ID, using the Async API.

If we want to start a test with 200K iterations and with a level of parallelism (threads) of 200, then we could enter this command:

```
java -jar couchbase-tests/target/couchbase-tests-exec.jar --iterations=200000 --threads=200 --host=localhost test1
```

Note that in this case, each unit of parallelism would perform 200K/200 = 1,000 iterations.

### Run Test2

Simple N1QL query by firstname, using the Async API, a primary index, and a secondary index on firstname. For improving the performance we have set up the option `adhoc=false` (prepared statements) and increased the default number of Query (N1QL) endpoints to 100.

If we want to start a test with 200K iterations and with a level of parallelism (threads) of 200, then we could enter this command:

```
java -jar couchbase-tests/target/couchbase-tests-exec.jar --iterations=200000 --threads=200 --host=localhost test2
```

Note that in this case, each unit of parallelism would perform 200K/200 = 1,000 iterations.


## MongoDB Tests

### Prepare data

Firstly, we will insert 200K documents in the database, entering this command:

```
java -jar mongodb-tests/target/mongodb-tests-exec.jar --iterations=200000 --host=localhost prepareData
```

By default it will create the documents in the `testdb` database, and in the `test-collection` collection.

### Run Test1

Simple `find()` by ID, using the Async API.

If we want to start a test with 200K iterations and with a level of parallelism (threads) of 200, then we could enter this command:

```
java -jar mongodb-tests/target/mongodb-tests-exec.jar --iterations=200000 --threads=200 --host=localhost test1
```

Note that in this case, each unit of parallelism would perform 200K/200 = 1000 iterations.

### Run Test2

Simple `find()` query by firstname, using a secondary index on firstname.

If we want to start a test with 200K iterations and with a level of parallelism (threads) of 200, then we could enter this command:

```
java -jar mongodb-tests/target/mongodb-tests-exec.jar --iterations=200000 --threads=200 --host=localhost test2
```

Note that in this case, each unit of parallelism would perform 200K/200 = 1000 iterations.

## Results of the tests

We have run all the tests in a standalone Virtual Machine with the following characteristics:
* VirtualBox machine running an Ubuntu 14.04.5 x86_64, 4GB of RAM, 4 CPUs
* Host machine: MacBook Pro Mid 2015, 2.2 GHz Intel Core i7, 16GB RAM 1600MHz DDR3

In all cases, we have performed 200K iterations with a level of parallelism of 200, so each unit of parallelism has performed 1,000 iterations.

With that configuration, I obtained the following results, in operations per second:

| Database                           | Test1         | Test2        |
| ---------------------------------- | ------------- | ------------ |
| Cauchbase 4.5.1 Enterprise Edition | 21,394/second | 2,268/second |
| Cauchbase 4.1.0 Community Edition  | 22,269/second | 540/second   |
| MongoDB 3.2.10 Community Server    |  9,581/second | 9,208/second |
