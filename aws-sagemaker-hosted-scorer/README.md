# DAI Deployment Template for Sagemaker Hosted Scorer

The code and documentation in this directory plugs in to the AWS SageMaker workflow documented here:

* https://docs.aws.amazon.com/sagemaker/latest/dg/your-algorithms-inference-code.html

It's a REST API which accepts one data point at a time for prediction in real-time in the hosted SageMaker environment.


## Overview

### Build Image

Generation of the Docker image is plugged into the build process of this project.
Run the following command in the root project directory to run the `build` process.

```bash
./gradlew :aws-sagemaker-hosted-scorer:jibDockerBuild
```

Verify that the Docker image was created, and take note of the version created.

```bash
docker images --format "{{.Repository}} \t {{.Tag}}" | grep "h2oai/rest-scorer"
```

### Optional: Test the build

After building, run to test the produced Docker container locally like this:

Step 1:  Put a pipeline.mojo and valid license.sig into this directory (aws-sagemaker-hosted-scorer).

Step 2:  Start the docker instance.

```
docker run \
    --rm \
    --init \
    -ti \
    -v `pwd`:/opt/ml/model \
    -p 8080:8080 \
    h2oai/dai-sagemaker-hosted-scorer \
    serve
```

Step 3:  Use curl to send a JSON-formatted row to the scorer as shown in the details below.


### Deploy to SageMaker

Create `h2oai/sagemaker-hosted-scorer` repository in Sagemaker for the scorer service image.

Use the output of the command below to `docker login`:

```
aws ecr get-login --region <region> --no-include-email
```

Tag the scorer service image for loading into the `h2oai/sagemaker-hosted-scorer` repository.

```
docker tag <IMAGE ID> <aws_account_id>.dkr.ecr.<region>.amazonaws.com/h2oai/sagemaker-hosted-server
```

Then push the scorer service image to AWS ECR (Elastic Container Registry):

```
docker push your-url.amazonaws.com/h2oai/sagemaker-hosted-scorer
```

Then create a model package with the pipeline file and the license key, and copy it to S3:

```
tar cvf mojo.tar pipeline.mojo license.sig
gzip mojo.tar
aws s3 cp mojo.tar.gz s3://your-bucket/
```

Next create the appropriate endpoint on Sagemaker.
Check that the endpoint is available with `aws sagemaker list-endpoints`.
See `example_request.py` for example of end point query.


## Details

### AWS Model Creation API

https://docs.aws.amazon.com/sagemaker/latest/dg/API_CreateModel.html

* CreateEndpoint
	* Environment
		* DRIVERLESS\_AI\_LICENSE\_KEY=base64key
	* ModelDataURL=s3://blah/blah/model.tar.gz


* `DRIVERLESS_AI_LICENSE_KEY` environment variable must contain the base64-encoded key (optional if a license.sig isns't included in mojo.tar.gz)
* `ModelDataURL` must point to an S3 URL with a .tar.gz file of the MOJO artifact


### Docker container

The docker container produced in this directory conforms to the specification described here:

* https://docs.aws.amazon.com/sagemaker/latest/dg/your-algorithms-inference-code.html#your-algorithms-inference-code-run-image

Sagemaker starts the container with the following command:

```
docker run image serve
```

Our container consists of the following entrypoint:

```
ENTRYPOINT ["java", "-jar", "serve.jar"]
```

You can test the container locally with the following curl command:

```
curl -X POST -H 'Content-Type: application/octet-stream' --data-binary @test.json http://localhost:8080/invocations
```

test.json:

```
{
    "field1" : "value1",
    "field2" : "value2"
}
```
