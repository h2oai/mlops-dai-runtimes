# Sagemaker Hosted Scorer


## Summary

The code and documentation in this directory plugs in to the AWS SageMaker workflow documented here:

* https://docs.aws.amazon.com/sagemaker/latest/dg/your-algorithms-inference-code.html

It's a REST API which accepts one data point at a time for prediction in real-time in the hosted SageMaker environment.


## Overview

### AWS Model Creation API

https://docs.aws.amazon.com/sagemaker/latest/dg/API_CreateModel.html

* CreateEndpoint
	* Environment
		* DRIVERLESS\_AI\_LICENSE\_KEY=base64key
	* ModelDataURL=s3://blah/blah/model.tar.gz


* `DRIVERLESS_AI_LICENSE_KEY` environment variable must contain the base64-encoded key
* `ModelDataURL` must point to an S3 URL with a .tar.gz file of the MOJO artifact


### Docker container

The docker container produced in this directory conforms to the specification described here:

* https://docs.aws.amazon.com/sagemaker/latest/dg/your-algorithms-inference-code.html#your-algorithms-inference-code-run-image

Sagemaker starts the container with the following command:

```
docker run image serve
```

## Details

Our container consists of the following entrypoint:

```
ENTRYPOINT ["java", "-jar", "sagemakerserve.jar"]
```

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
