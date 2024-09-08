# DAI Deployment Template for Local SpringBoot Scorer

This package contains sources of a generic Java scorer implementation based on SpringBoot
and its Docker image.

## Building

The code of the local SpringBoot scorer is a gradle project build as usual by
`./gradlew build`.

The resulting executable jar is located in the `build/libs` folder.

## Running

To run the local scorer, you can either use `bootRun` gradle task or run directly the executable jar:

```bash
java -Dmojo.path={PATH_TO_MOJO_PIPELINE} -jar build/libs/local-rest-scorer-{YOUR_CURRENT_VERSION}-boot.jar
``` 

To run the local scorer with shapley contribution enabled
```bash
# Equivalent to using below and setting `-Dshapley.types.enabled=ALL`
java -Dmojo.path={PATH_TO_MOJO_PIPELINE} -Dshapley.enable=true -jar build/libs/local-rest-scorer-{YOUR_CURRENT_VERSION}-boot.jar

# SHAPLEY_TYPE can be one of [ALL, TRANSFORMED, ORIGINAL]
java -Dmojo.path={PATH_TO_MOJO_PIPELINE} -Dshapley.types.enabled={SHAPLEY_TYPE} -jar build/libs/local-rest-scorer-{YOUR_CURRENT_VERSION}-boot.jar
``` 

Note: Current Mojo2 shapley implementation means that the mojo must be loaded one
time for each shapley value type. This means that if `-Dshapley.types.enabled=ALL`
the mojo will be loaded three times:

1. for standard scoring
2. for scoring shapley values on transformed features
3. for scoring shapley values on original features

This is important because, depending on the size of the mojo, the scorer may run out of memory if `ALL` is set.
``` 

> Tip: If you run into an error loading the MOJO, ensure you specify its full path and are not triggering shell expansion (e.g. avoid the `~` character).

### Enable Https

Springboot application come with built-in support for HTTPS. You can get more information from the
Springboot [documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto.webserver.configure-ssl)

The servers support using either JKS or PKSC12 type certificates to secure network traffic. 
If you do not have certificates, you can create a self-signed SSL Certificate
with java built in tool `keytool` or via `openssl`. See some examples [below](#using-or-creating-self-signed-certificates).


#### Simple Configuration

Configuration of SSL requires several parameters to be set for the application:

```
# Required
server.ssl.enabled=true
server.ssl.key-store=/path/to/keystore.jks
server.ssl.key-password=<enter password>

# Optional
server.port=<default port is 8080>
server.ssl.key-alias=<alias associated with keystore provided>
server.ssl.key-store-provider=SUN  # can be SUN for example
server.ssl.key-store-type=JKS  # JKS, PKCS12
```

These can be set in an application.properties file or via the command line. 

* Deploy via command line:

```shell
java -Dmojo.path=/path/to/pipeline.mojo \
     -jar /path/to/local-rest-scorer.jar \
     --server.ssl.enabled=true \
     --server.ssl.key-store=/path/to/keystore.jks \
     --server.ssl.key-password=mypassword
```

* Deploy via command line with shapley contribution enabled:

```shell
java -Dmojo.path=/path/to/pipeline.mojo \
     -Dshapley.enable=true \
     -jar /path/to/local-rest-scorer.jar \
     --server.ssl.enabled=true \
     --server.ssl.key-store=/path/to/keystore.jks \
     --server.ssl.key-password=mypassword
```

* Deploy with `application.properties` file. See [here](./examples/application.properties) for example `application.properties` file.

```shell
java -Dmojo.path=/path/to/pipeline.mojo \
     -Dspring.config.location=/path/to/application.properties
     -jar /path/to/local-rest-scorer.jar
```

* Deploy with shapley contribution enabled

```shell
java -Dmojo.path=/path/to/pipeline.mojo \
     -Dshapley.enable=true \
     -Dspring.config.location=/path/to/application.properties
     -jar /path/to/local-rest-scorer.jar
```

#### Using or Creating Self-Signed Certificates

Below are some examples of how to create a self-signed certificate using `keytool` and 
how to convert a preexisting `PEM` based certificate into a useable `PKSC12` certificate.

```shell
# Example: using keytool to create a JKS certificate
keytool -genkey \
        -alias selfsigned_localhost_sslserver \
        -keyalg RSA -keysize 2048 \
        -validity 700 \
        -keypass <changeit> \
        -storepass <changeit> \
        -keystore ssl-server.jks
```

If you already have a PEM based certificate pair, you can convert it to PKCS12 and use it with the 
server using the following:

```shell
# Example: using openssl to convert a preexisting .pem certificate pair into a usable PKCS12 certificate 
# NOTE: This will ask for a password that will you will need to provide in server configurations
openssl pkcs12 -export \
    -in /path/to/server-certificate.pem \
    -inkey /path/to/server-key.pem \
    -name scorer \
    -out /path/to/scorer.p12
```

NOTE: remember to set `server.ssl.key-store-type=PKSC12` in the server configurations if you want to
use PKSC12 type certificates

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

For example:
```bash
curl -X GET http://localhost:8080/model/sample_request | curl -X POST \
  -H "Content-Type: application/json" \
  -d @- http://localhost:8080/model/score
```

### API Inspection

You can use SpringFox endpoints that allow both programmatic and manual inspection of the API:

* Swagger JSON representation for programmatic access: http://localhost:8080/v2/api-docs.
* The UI for manual API inspection: http://localhost:8080/swagger-ui/index.html.

## Docker Image

Docker image for this REST scorer is built using
[Jib](https://github.com/GoogleContainerTools/jib).

### Build Image

Generation of this Docker image is plugged into the build process of this project.
Run the following command in the root project directory to run the `build` process.

```bash
docker build -t rest-scorer .
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
