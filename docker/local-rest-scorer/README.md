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
  -v /path/to/local/pipeline.mojo:/mojos/pipeline.mojo \
  -v /path/to/local/license.sig:/secrets/license.sig \
  -p 8080:8080 \
  -t h2oai/rest-scorer:<version>
```

Notice how the desired MOJO was mounted to the container:
```
-v /path/to/local/pipeline.mojo:/mojos/pipeline.mojo
```

Notice how your H2O.ai DriverlessAI license was mounted to the container:
```
-v /path/to/local/license.sig:/secrets/license.sig
```


Alternatively, you could pass in your license as an environment variable:

> Note: However, this approach is not recommended as your license may be written to system logs.

```bash
docker run \
  --name rest-scorer \
  -v /path/to/local/pipeline.mojo:/mojos/pipeline.mojo \
  -e DRIVERLESS_AI_LICENSE_KEY=12345abcde \
  -p 8080:8080 \
  -t h2oai/rest-scorer:<version>
```

### 2. Score!

A mojo scorer is now running on http://localhost:8080.

Refer to the [local rest scorer README](https://github.com/h2oai/dai-deployment-templates/blob/master/local-rest-scorer/README.md) for information on how to score requests.
