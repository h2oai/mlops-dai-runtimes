# DAI Deployment Template for Local SpringBoot Scorer

This package contains sources of a generic Java scorer implementation based on SpringBoot.


## Building

The code of the local SpringBoot scorer is a gradle project build as usual by
`./gradlew build`.

The resulting executable jar is located in the `build/libs` folder.


## Running

To run the local scorer, you can either use `bootRun` gradle task or run directly the executable jar:

```bash
$ java -Dmojo.path={PATH_TO_MOJO_PIPELINE} -jar build/libs/local-rest-scorer-{YOUR_CURRENT_VERSION}.jar
``` 


### Score JSON Request

To test the endpoint, send a request to http://localhost:8080 as follows:

```bash
$ curl \
    -X POST \
    -H "Content-Type: application/json" \
    -d @test.json http://localhost:8080/models/{UUID_OF_YOUR_MOJO_PIPELINE}/score
```

You can get the UUID of the loaded pipeline by calling the following:

```bash
$ curl http://localhost:8080/models
```

Which should return a json list with the UUID.

Alternatively, the scorer log contains the UUID as well in the form:
`Mojo pipeline successfully loaded (a12e7390-b8ac-406a-ade9-0d5ea4b63ea9).`
The hex string in parenthesis is the UUID of you mojo pipeline.

This expects a file `test.json` with the actual scoring request payload.
If you are using the mojo trained in `test/data/iris.csv` as suggested above,
you should be able to use the following json payload:

```json
{
  "fields": [
    "sepal_len", "sepal_wid", "petal_len", "petal_wid"
  ],
  "includeFieldsInOutput": [
    "sepal_len"
  ],
  "rows": [
    [
      "1.0", "1.0", "2.2", "3.5"
    ],
    [
      "3.0", "10.0", "2.2", "3.5"
    ],
    [
      "4.0", "100.0", "2.2", "3.5"
    ]
  ]
}
```

The expected response should follow this structure, but the actual values may differ:

```json
{
  "score": [
    [
      "1.0",
      "0.6240277982943945",
      "0.045458571508101536",
      "0.330513630197504"
    ],
    [
      "3.0",
      "0.7209441819603676",
      "0.06299909138586585",
      "0.21605672665376663"
    ],
    [
      "4.0",
      "0.7209441819603676",
      "0.06299909138586585",
      "0.21605672665376663"
    ]
  ]
}
```


### Score CSV File

Alternatively, you can score an existing file on the local filesystem using `GET` request to the same endpoint:

```bash
$ curl \
    -X GET \
    http://localhost:8080/models/{UUID_OF_YOUR_MOJO_PIPELINE}/score/?file=/tmp/test.csv
```

This expects a CSV file `/tmp/test.csv` to exist on the machine where the scorer runs (i.e., it is not send to it
over HTTP).


### API Inspection

You can use SpringFox endpoints that allow both programmatic and manual inspection of the API:

* Swagger JSON representation for programmatic access: http://localhost:8080/v2/api-docs.
* The UI for manual API inspection: http://localhost:8080/swagger-ui.html.

