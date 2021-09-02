# Driverless AI Deployment Template for Local SpringBoot Scorer

This [template](https://github.com/h2oai/dai-deployment-templates/tree/master/local-rest-scorer) contains sources of a generic Java scorer implementation for `Driverless AI MOJO scoring pipiline` with `Java runtime`, based on SpringBoot and its Docker image.

This scorer can be used to do Real-time scoring on single or multiple rows. The user needs to provide Driverless AI license key and  model's  pipeline.mojo file for scoring. The versions of software used to create the template like mojo runtime are listed [here](https://github.com/h2oai/dai-deployment-templates/blob/master/gradle.properties#L8).

## Building

The code of the local SpringBoot scorer is a gradle project build as usual by
`./gradlew build`.

The resulting executable jar is located in the `build/libs` folder.

## Running

To run the local scorer, you can either use `bootRun` gradle task or run directly the executable jar:

```bash
java -Dmojo.path=/Users/raji/Downloads/mojo-pipeline -jar build/libs/local-rest-scorer-{YOUR_CURRENT_VERSION}-boot.jar
``` 
To get shapley contribution 
```bash
java -Dmojo.path=/Users/raji/Downloads/mojo-pipeline  -Dh2oai.scorer.enable.shapley=true -jar build/libs/local-rest-scorer-{YOUR_CURRENT_VERSION}-boot.jar
``` 

### Score JSON Request

To test the endpoint, send a request to http://localhost:8080 as follows:

```bash
curl \
    -X POST \
    -H "Content-Type: application/json" \
    -d @test.json http://localhost:8080/model/score
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
  "id": "a12e7390-b8ac-406a-ade9-0d5ea4b63ea9",
  "fields": [
    "sepal_len",
    "class.Iris-setosa",
    "class.Iris-versicolor",
    "class.Iris-virginica"
  ],
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

Note that including the `fields` in the response can be disabled by setting `noFieldNamesInOutput`
to true in the input request.

### Score CSV File

Alternatively, you can score an existing file on the local filesystem using `GET` request to the same endpoint:

```bash
curl -X GET http://localhost:8080/model/score/?file=/tmp/test.csv
```

This expects a CSV file `/tmp/test.csv` to exist on the machine where the scorer runs (i.e., it is not send to it
over HTTP).

### Model ID

You can get the UUID of the loaded pipeline by calling the following:

```bash
$ curl http://localhost:8080/model/id
```

Which should return the UUID of the loaded mojo model.

Alternatively, the scorer log contains the UUID as well in the form:
`Mojo pipeline successfully loaded (a12e7390-b8ac-406a-ade9-0d5ea4b63ea9).`
The hex string in parenthesis is the UUID of you mojo pipeline.

### Get Example Request

The scorer can also provide an example request that would pass all validations.
This way, users can quickly get an example scoring request to send to the scorer to test it.
This request can be further filled with meaningful input values.

```bash
curl -X GET http://localhost:8080/model/sample_request
```

The resulting JSON is a valid input for the POST `/model/score` request.

### API Inspection

You can use SpringFox endpoints that allow both programmatic and manual inspection of the API:

* Swagger JSON representation for programmatic access: http://localhost:8080/v2/api-docs.
* The UI for manual API inspection: http://localhost:8080/swagger-ui.html.

## Docker Image

Docker image for this REST scorer is built using
[Jib](https://github.com/GoogleContainerTools/jib).

### Build Image

Generation of this Docker image is plugged into the build process of this project.
Run the following command in the root project directory to run the `build` process.

```bash
./gradlew :local-rest-scorer:jibDockerBuild
```

Verify that the Docker image was created, and take note of the version created.
```bash
docker images --format "{{.Repository}} \t {{.Tag}}" | grep "h2oai/rest-scorer"
```

### Run Container

> Note: Replace `<version>` with the version of the image you found from the previous step.

```bash
docker run \
  --name rest-scorer \
  -v /path/to/local/pipeline.mojo:/mojos/pipeline.mojo:ro \
  -v /path/to/local/license.sig:/secrets/license.sig:ro \
  -p 8080:8080 \
  h2oai/rest-scorer:<version>
```

Notice how the desired MOJO was mounted to the container:
```
-v /path/to/local/pipeline.mojo:/mojos/pipeline.mojo:ro
```

Notice how your H2O.ai DriverlessAI license was mounted to the container:
```
-v /path/to/local/license.sig:/secrets/license.sig:ro
```

Alternatively, you could pass in your license as an environment variable:

First, `export` your license key.
```bash
read -s DRIVERLESS_AI_LICENSE_KEY < /path/to/local/license.sig
export DRIVERLESS_AI_LICENSE_KEY
```

> Note: Option `-s`, above, hides the echoing of your license so that its content is not written to logs.

Now start a container.

```bash
docker run \
  --name rest-scorer \
  -v /path/to/local/pipeline.mojo:/mojos/pipeline.mojo:ro \
  -e DRIVERLESS_AI_LICENSE_KEY \
  -p 8080:8080 \
  h2oai/rest-scorer:<version>
```

See section [Running](#running) above for information on how to score requests.
