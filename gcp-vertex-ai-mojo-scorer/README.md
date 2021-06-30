# DAI Deployment Template for GCP Vertex AI

The docker image that is built by this project can be pushed to gcr.io and used for scoring
Driverless AI Mojos in GCP Vertex AI. 

## Building

Make sure you are in the working directory `dai-deployment-templates`. Typing `pwd` in the terminal
shell should have a similar output to `/my/path/to/dai-deployment-templates`.

You can build the Docker image in two ways. The first only includes the ability to score and the second
includes the ability to set a preprocessing script. The difference between the two options is the base
image used. If you choose to include Python and the option for the preprocessing script, then the base
image used is `openkbs/jre-mvn-py3:latest`. Otherwise, the base image used is `openjdk:8-jre-alpine`.

* Run the following command to build without preprocessing script option: 
  ```shell script
  ./gradlew build -PdockerRepositoryPrefix=gcr.io/your/repository -PdockerUsePython=false
  ```
  and the docker image required for GCP Vertex AI will be in the directory `gcp-vertex-ai-mojo-scorer/build`:
  ```shell script
  /path/to/dai-deployment-templates/gcp-vertex-ai-mojo-scorer/build/jib-image.tar
  ```

* Run the following command to build with preprocessing script option:
```shell script
./gradlew build -PdockerRepositoryPrefix=gcr.io/your/repository
```
and the docker image required for GCP Vertex AI will be in the directory `gcp-vertex-ai-mojo-scorer/build`:
```shell script
/path/to/dai-deployment-templates/gcp-vertex-ai-mojo-scorer/build/jib-image.tar
```

* Load the resulting `jib-image.tar` file to docker
```shell script
docker load < /path/to/dai-deployment-templates/gcp-vertex-ai-mojo-scorer/build/jib-image.tar
``` 

* Follow the steps explained here in Google Documentation: https://cloud.google.com/run/docs/building/containers, to 
push the image to gcr.io.

## Deploying

To deploy the container follow the steps in Google Documentation here to import the model:
https://cloud.google.com/vertex-ai/docs/general/import-model

There is one requirement for the container. You __MUST__ include the following environment variables:
* MOJO_GCS_PATH = `gs://path/to/pipeline.mojo`
* LICENSE_GCS_PATH = `gs://path/to/driverless/ai/license.sig`

If you built the Docker image with the preprocessing script option, you also should include the following environment variable:
* PREPROCESSING_SCRIPT_PATH = `gs://path/to/preprocessing_script.py`
If the environment variable `PREPROCESSING_SCRIPT_PATH` is not included, then the Docker image will not use
a preprocessing script. In which case, we highly recommend that you build the image without Python included.

The prediction route will be `/model/score` and the health route will be `/model/id`.

You can then deploy an endpoint once the model has been imported.

## Preprocessing Script

If you are including a Python data preprocessing script, you can find an example in the exampels folder here: [preprocessing_script_example.py](examples/preprocessing_script_example.py)

The deployed Docker image will pass the original request and data through a JSON file to the provided Python preprocessing script. The name of the JSON file is passed to the preprocessing script as a command line argument. The JSON file is located at `/tmp`. The preprocessing script will have to overwrite the original JSON (just like in the example above) with the modified data. Therefor, there is some disk IO that is required when using a preprocessing script.

## Scoring

Follow the instructions provided for scoring using the deployed endpoint for your model.

A sample of the JSON passed with the request to the endpoint looks like:

```
{
  "instances": [
    [1046.0, -6.759521484375, -5.128787040710449, -4.707362174987793, -3.5251803398132324, -4.9632248878479, -3.131549835205078, -6.017838478088379, -10.457297325134276, -4.679401874542236, -5.052502155303955, -2.327012062072754, -3.8113958835601807, -3.1215708255767822, -3.0776915550231934, -2.41096305847168, -3.1130094528198238, -1.782581806182861, -3.1492466926574707, -2.1638123989105225, -1.9953984022140503, -2.255126476287842, -1.6565601825714111, -1.7830057144165041, -2.0690438747406006, -1.6889469623565674, -1.3084837198257446, -5.775942802429199, -1.2526675462722778, 0.009999999776482582],
    [1542.0, -13.192670822143555, -9.38182258605957, -4.743411064147949, -3.3766469955444336, -3.9347853660583496, -2.774277687072754, -18.75088882446289, -3.955642461776733, -2.7279736995697017, -5.052502155303955, -2.4862608909606934, -8.042284965515138, -2.344266176223755, -9.072710990905762, -2.3926336765289307, -7.395956993103027, -1.782581806182861, -3.5251612663269043, -2.1638123989105225, -3.4930498600006104, -6.524789810180664, -1.6876335144042969, -2.8874309062957764, -2.103355884552002, -1.59183669090271, -1.085852026939392, -3.017481803894043, -1.0860896110534668, 0.7699999809265137]
  ],
  "parameters": {"fields": ["Time", "V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9", "V10", "V11", "V12", "V13", "V14", "V15", "V16", "V17", "V18", "V19", "V20", "V21", "V22", "V23", "V24", "V25", "V26", "V27", "V28", "Amount"]}
}
```

And a sample request would look like:

```shell script
curl \
-X POST \
-H "Authorization: Bearer $(gcloud auth print-access-token)" \
-H "Content-Type: application/json" \
https://us-central1-aiplatform.googleapis.com/v1alpha1/projects/${PROJECT_ID}/locations/us-central1/endpoints/${ENDPOINT_ID}:predict \
-d "@${INPUT_DATA_FILE}"
```
