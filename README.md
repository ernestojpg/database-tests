
# Database Tests

## Compile project

You will need JDK 8, and Maven 3.x

Just enter:

```
mvn clean package
```

## Tests

Firstly, for all tests we will insert in the Database a given number N of Json documents with this structure:

```json
// ID: walter-<num>
{
  "firstname": "Walter <num>",
  "lastname": "White",
  "job": "chemistry teacher",
  "age": 50
}
```

Where `<num>` takes values from 0 to N-1.

In all cases, we will perform two main tests:
* Get all documents one by one, by their ID
* Search all documents one by one, by their firstname.

## Couchbase Tests

### Prepare data

Firstly, we will insert 200K documents in the database, entering this command:

```
java -jar couchbase-tests/target/couchbase-tests-exec.jar --iterations=200000 --host=localhost prepareData
```

### Run Test1

If we want to start a test with 200K iterations and with a level of parallelism (threads) of 200, then we could enter this command:

```
java -jar couchbase-tests/target/couchbase-tests-exec.jar --iterations=200000 --threads=200 --host=localhost test1
```

Note that in this case, each unit of parallelism would perform 200K/200 = 1000 iterations.

### Run Test2

If we want to start a test with 200K iterations and with a level of parallelism (threads) of 200, then we could enter this command:

```
java -jar couchbase-tests/target/couchbase-tests-exec.jar --iterations=200000 --threads=200 --host=localhost test2
```

Note that in this case, each unit of parallelism would perform 200K/200 = 1000 iterations.

## MongoDB Tests

### Prepare data

Firstly, we will insert 200K documents in the database, entering this command:

```
java -jar mongodb-tests/target/mongodb-tests-exec.jar --iterations=200000 --host=localhost prepareData
```

### Run Test1

If we want to start a test with 200K iterations and with a level of parallelism (threads) of 200, then we could enter this command:

```
java -jar mongodb-tests/target/mongodb-tests-exec.jar --iterations=200000 --threads=200 --host=localhost test1
```

Note that in this case, each unit of parallelism would perform 200K/200 = 1000 iterations.

### Run Test2

If we want to start a test with 200K iterations and with a level of parallelism (threads) of 200, then we could enter this command:

```
java -jar mongodb-tests/target/mongodb-tests-exec.jar --iterations=200000 --threads=200 --host=localhost test2
```

Note that in this case, each unit of parallelism would perform 200K/200 = 1000 iterations.

## Tests

Running the tests in the following machine:
* VirtualBox machine running an Ubuntu 14.04.5 x86_64, 4GB of RAM, 4 CPUs
* Host machine: MacBook Pro, 2.2 GHz Intel Core i7, 16GB RAM

I obtained the following results, in operations per second:

| Database                           | Test1         | Test2        |
| ---------------------------------- | ------------- | ------------ |
| Cauchbase 4.5.1 Enterprise Edition | 21,394/second | 2,268/second |
| Cauchbase 4.1.0 Community Edition  | 22,269/second | 540/second   |
| MongoDB 3.2.10 Community Server    |  9,581/second | 9,208/second |
