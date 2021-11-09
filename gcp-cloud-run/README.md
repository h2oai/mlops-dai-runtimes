# DAI Deployment Template for Google Cloud Run

This package extends the implementation of Local Rest Scorer [here](../local-rest-scorer).

The docker image that is built by this project can be pushed to gcr.io and used for scoring
Driverless AI Mojos in [Google Cloud Run](https://cloud.google.com/run). 

## Building

Since there is a direct dependency on the separate, above mentioned project [local-rest-scorer](../local-rest-scorer) 
it is best to build this project from the root directory.

Make sure you are in the working directory `dai-deployment-templates`. Typing `pwd` in the terminal
shell should have a similar output to `/my/path/to/dai-deployment-templates`.

* Run the following command: 
  ```shell script
  ./gradlew build
  ```
  and the docker image required for Google Cloud Run will be in the directory `gcp-cloud-run/build`:
  ```shell script
  /path/to/dai-deployment-templates/gcp-cloud-run/build/jib-image.tar
  ```

* Load the resulting `jib-image.tar` file to docker
  ```shell script
  docker load < /path/to/dai-deployment-templates/gcp-cloud-run/build/jib-image.tar
  ``` 

* Follow the steps explained here in Google Documentation: https://cloud.google.com/run/docs/building/containers, to 
push the container to gcr.io

## Deploying

To deploy the container follow the steps in Google Documentation here:
https://cloud.google.com/run/docs/deploying

There is one requirement for the container. You __MUST__ include the following environment variables:
* MOJO_GCS_PATH = `gs://path/to/pipeline.mojo`
* LICENSE_GCS_PATH = `gs://path/to/driverless/ai/license.sig`

## Scoring

On a successful deployment to Google Cloud Run, you will be provided an endpoint that can be scored against.

The api for scoring is the same as the [Local Rest Scorer](../local-rest-scorer)

To access api information: `https://<google provided endpoint>/swagger-ui/index.html`

To score the model: `https://<google provided endpoint>/model/score`

Sample curl request:
```shell script
curl -X POST -H "Content-Type: application/json" \
     -d '{"includeFieldsInOutput":null,"noFieldNamesInOutput":null,"idField":null, \ 
          "fields":["LIMIT_BAL","SEX","EDUCATION","MARRIAGE","AGE","PAY_1","PAY_2", \
                    "PAY_3","PAY_4","PAY_5","PAY_6","BILL_AMT1","BILL_AMT2","BILL_AMT3", \
                    "BILL_AMT4","BILL_AMT5","BILL_AMT6","PAY_AMT1","PAY_AMT2","PAY_AMT3", \
                    "PAY_AMT4","PAY_AMT5","PAY_AMT6"], \
          "rows":[["0","text","text","text","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0"]]}' \
     https://<google provided endpoint>/model/score

```