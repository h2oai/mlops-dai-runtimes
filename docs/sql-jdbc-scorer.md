# Driverless AI Deployment Template for Local Rest SQL Scorer

This [template](https://github.com/h2oai/dai-deployment-templates/tree/master/sql-jdbc-scorer) contains an implementation of generic Java implementation
for scoring Driverless AI Mojos(
java runtime) against a SQL database. The application runs
as a restful service and receives requests that include a SQL query and
additional, appropriate parameters for scoring the table that results from the
SQL query, and writing the preditions back to the database. 

The user needs to provide Driverless AI license key and  model's pipeline.mojo file for scoring. The versions of software used to create the template like mojo runtime are listed [here](https://github.com/h2oai/dai-deployment-templates/blob/master/gradle.properties).

## Implementation

The implementation leverages Spark APIs to handle data ingest/export via JDBC and
Sparkling Water API's to manage distributed scoring in a Spark Session. This decision
was made in order to allow for larger queries as Spark can manage data partitions 
very cleanly and avoid OOM errors.

## Building

From the root project of this repository. You can run the following:
`./gradlew build`

This will build the entire project and resultant jar file required for running the
application will be available in the directory: [build/libs](../sql-jdbc-scorer/build/libs)

## Running

In order to run the application you will need 4 files in addition to the `jar` file:

1. Driverless AI License file - typically: `license.sig`
2. Driverless AI Mojo - typically: `pipeline.mojo`
3. JDBC configuration file - typically: `jdbc.conf`, example found here: 
[jdbc.conf](../sql-jdbc-scorer/examples/conf/jdbc.conf) 
    - Note: the configuration file contains the expected JDBC driver
     class name `org.postgresql.Driver` which applies to the example below. 
4. JDBC Driver jar - the below example uses `postgresql-42.2.5.jar`, but this should
be what ever JDBC driver jar is used by the database in question. 

These files will be added to the JVM via system properties using `-D` flag. Example run command:

```bash
java -cp sql-jdbc-scorer-1.0.6-SNAPSHOT.jar \
 -Dmojo.path=pipeline.mojo \
 -Djdbc.config.path=jdbc.conf \
 -Dloader.path=postgresql-42.2.5.jar \
 -Dloader.main=ai.h2o.mojos.deploy.sql.db.ScorerApplication \
 org.springframework.boot.loader.PropertiesLauncher
```

### Score Request

There are 2 methods of scoring. Both with the same end result:

1. GET - takes input parameters in GET query and scores resultant dataset to the configured database
2. POST - takes input json payload and scores resultant dataset to the configured database

Parameter inputs for both methods:

- **sqlQuery**: String representation of a SQL Query. Ex. `SELECT * FROM myTable`
- **outputTable**: String representation of destination table for scorer to write to
- **idColumn**: numeric unique key for scorer to use for proper data partitioning.
also can be used as key to join on with original table
    - **Note**: this can be an empty string `"""`, but if no idColumn is provided there
    is a possibility that the application will run out of memory if a very large query is
    provided. Additionally, the resultant table will only contain the prediction columns
    making it hard to reference against the original table. 
- **saveMethod**: one of `[preview, append, overwrite, ignore, error]` each with different behavior:
  - preview: does not write to the database, only gives preview of resultant scored dataset in response
  - All others are clearly documented as part of Spark API: 
  [here](https://spark.apache.org/docs/latest/api/java/index.html?org/apache/spark/sql/SaveMode.html)

##### GET

example query:

```bash
http://localhost:8080/model/score?sqlQuery=%22SELECT%20*%20FROM%20creditcardtrain%22&outputTable=%22helloworld%22&idColumn=%22id%22
```

##### POST

example query:

```bash
curl -X POST -H "Content-Type: application/json" \
-d '{"query": "SELECT * FROM creditcardtrain LIMIT 10", "idColumn": "id", "saveMethod": "overwrite", "outputTable": "helloworld"}' \
http://localhost:8080/model/score
```

### Model ID

You can get the UUID of the loaded pipeline by calling the following:

```bash
$ curl http://localhost:8080/model/id
```

### API Inspection

You can use SpringFox endpoints that allow both programmatic and manual inspection of the API:

* Swagger JSON representation for programmatic access: http://localhost:8080/v2/api-docs.
* The UI for manual API inspection: http://localhost:8080/swagger-ui/index.html.

