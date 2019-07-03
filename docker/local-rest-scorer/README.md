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

### 2. Score!

A mojo scorer is now running on http://localhost:8080.

Refer to the [local rest scorer README](https://github.com/h2oai/dai-deployment-templates/blob/master/local-rest-scorer/README.md) for information on how to score requests.
