# DAI Deployment Template for Local SpringBoot Scorer

This package contains sources of a generic Java scorer implementation based on SpringBoot.


## Building

The code of the local SpringBoot scorer is a gradle project build as usual by
`./gradlew build`.

> TODO(osery): Describe how we get the executable SpringBoot jar, which needs to be embedded in the
> distribution zip archive.

> TODO(osery): Describe score configuration to pass in the model(s), the license key, and the port.


## Running

> TODO(osery): How to best run the scorer locally + what params to pass in.

To test the endpoint, send a request to http://localhost:8080 as follows:

```bash
$ curl \
    -X POST \
    -d @test.json http://localhost:8080
```

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

Alternatively, you can use SpringFox endpoints that allow both programmatic and manual inspection
of the API:

* Swagger JSON representation for programmatic access: http://localhost:8080/v2/api-docs.
* The UI for manual API inspection: http://localhost:8080/swagger-ui.html.

