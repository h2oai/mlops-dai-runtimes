# DAI Deployment Template for GCP Vertex AI

The docker image that is built by this project can be pushed to gcr.io and used for scoring
Driverless AI Mojos in GCP Vertex AI. 

## Building

Make sure you are in the working directory `dai-deployment-templates`. Typing `pwd` in the terminal
shell should have a similar output to `/my/path/to/dai-deployment-templates`.

You can build the Docker image in two ways. The first only includes the ability to score and the second
includes the ability to set a preprocessing script.

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

If you built the Docker image with the preprocessing script option, you also __MUST__ include the following environment variable:
* PREPROCESSING_SCRIPT_PATH = `gs://path/to/preprocessing_script.py`

The prediction route will be `/model/score` and the health route will be `/model/id`.

You can then deploy an endpoint once the model has been imported.

## Preprocessing Script

If you are including a Python data preprocessing script, here is an example:

```
#!/usr/bin/env python
# coding: utf-8
import json
import sys

fileName = str(sys.argv[1])

# Load JSON file with request data
with open("/tmp/"+fileName) as f:
    request = json.load(f)
    
data = [
    [69338.0,0.7315255726985329,-0.5847660267950631,0.386082437076677,0.821180632502935,-0.0312602331172659,1.1980865440065802,-0.13618956328652698,0.391451241672127,0.3738906601386479,-0.461823737560995,0.625861374468075,1.07615129929773,0.0718516496509022,0.0620528341180761,0.850326761944984,-1.6339872927690802,1.3467406501738701,-2.7597098786627994,-1.5048495641502198,0.0533811518528488,0.00272572637419962,0.00836864608482666,0.0456492844845917,-0.5780279514412049,0.0478507743996201,0.36036065876766993,0.0224220501004547,0.0282128845008635,157.49],
    [81081.0,-0.873286664580274,0.5383418573374871,2.65055031553525,0.331972130017534,0.0510277957117218,0.6364468979718391,0.6719139650867261,0.18165642237906401,0.0793536531651418,-0.684037044169426,0.8644669396918221,0.8734041370577021,-0.7131054601776229,-0.494781246049671,-1.94685930894166,-0.6014164255583699,-0.0460304794694537,-0.356068770928204,-0.537032123913058,-0.0619970210088985,-0.00642393408573803,0.32916282786391804,-0.33233023038250503,0.22704204810440198,0.546100808417714,-0.40064788122765005,-0.0896039366748501,-0.166155749695546,38.0]
]

# Replace data with new samples
request["instances"] = data

# Dump new request to JSON
with open("/tmp/"+fileName, 'w') as f:
    json.dump(request, f)
```

The deployed Docker image will pass the original request and data through a JSON file to the provided Python preprocessing script. The name of the JSON file is passed to the preprocessing script as a command line argument. The JSON file is located at `/tmp`. The preprocessing script will have to overwrite the original JSON (just like in the example above) with the modified data.

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
