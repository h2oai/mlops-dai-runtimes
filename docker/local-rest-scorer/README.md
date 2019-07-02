# Docker Image - REST Scorer

This is a Docker image for the [local rest scorer](https://github.com/h2oai/dai-deployment-templates/tree/master/local-rest-scorer).

## How To Build

Generation of this Docker image is plugged into the build process of this project.  Run the following command in the root project directory to run the `build` process.

```bash
./gradlew build
```

Verify that the Docker image was created, and take note of the version created.
```bash
docker images --format "{{.Repository}} \t {{.Tag}}" | grep "h2oai/rest-scorer"
```


## How To Run

### 1. Start the Docker container

> Note: replace `<version>` with the version of the image you found from the previous step.

```bash
docker run \
  --name rest-scorer \
  --rm \
  -p 8080:8080 \
  -t h2oai/rest-scorer:<version>
```

### 2. Upload your DAI license

The Docker image expects a H2O.ai DriverlessAI license to be located at `/root/license.sig`.

```bash
docker cp /path/to/license.sig rest-scorer:/root/license.sig
```

### 3. Upload your MOJO

The scoring application continuously looks for a DriverlessAI mojo to be uploaded to `/tmp/pipeline.mojo`

```bash
docker cp /path/to/pipeline.mojo rest-scorer:/tmp/pipeline.mojo
```

### 4. Score!

A mojo scorer is now running on http://localhost:8080.

Refer to the [local rest scorer README](https://github.com/h2oai/dai-deployment-templates/blob/master/local-rest-scorer/README.md) for information on how to score requests.
