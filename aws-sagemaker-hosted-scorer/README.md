# SageMaker Hosted Scorer


## Summary

The code and documentation in this directory plugs in to the AWS SageMaker workflow documented here:

* https://docs.aws.amazon.com/sagemaker/latest/dg/your-algorithms-inference-code.html

It's a REST API which accepts one data point at a time for prediction in real-time in the hosted SageMaker environment.


## Overview

### Build process (tested on Linux x86_64 with Docker)

Follow these steps to build.  The build is fully Dockerized, so you should not need to install anything locally except for Docker.

```
make clean
make
```

The output is a docker container:

`h2oai/dai-sagemaker-hosted-scorer:latest`

### Optional: Test the build

After building, run to test the produced Docker container locally like this:

Step 1:  Put a pipeline.mojo into this directory (aws-sagemaker-hosted-scorer).

Step 2:  Start the docker instance.

```
DRIVERLESS_AI_LICENSE_KEY=<paste key here> make run
```

Step 3:  Use curl to send a JSON-formatted row to the scorer as shown in the details below.


### Deploy to SageMaker

(Note:  These steps are still incomplete, other stuff was done in the UI first.  Needs some cleanup.)

You need to `docker login` with the output from the command below:

```
aws ecr get-login --region us-west-2 --no-include-email
```

Then push the scorer service image to AWS ECR (Elastic Container Registry):

```
docker push your-url.amazonaws.com/h2oai/dai-sagemaker-hosted-scorer:latest
```

Then create a model package with the pipeline file and the license key, and copy it to S3:

```
tar cvf mojo.tar pipeline.mojo license.sig
gzip mojo.tar
s3cmd put pipeline.mojo.tar.gz s3://h2oai-tomk-us-west-2/sagemaker-test/pipeline.mojo.tar.gz
```


## Details

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


## Simple standalone REST server without SageMaker

There is a second endpoint, predict_parameters, which you can use with just
a vanilla HTTP GET and specifying model parameters with the query string
instead of with a JSON payload.

This is handy for local testing.  After building the image, you can run it
locally as follows:

```
DRIVERLESS_AI_LICENSE_KEY=<paste key here> java -jar build/libs/serve.jar
```

(You can put the license key in ~/.driverlessai/license.sig instead if that is easier.)

This opens an HTTP server on port 8080.

You can then send traffic to the server using standard tools like curl:

```
curl -v "http://localhost:8080/predict_parameters?field1=value1&field2=value2"
```

output:

```
* Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /predict_parameters?step=0&customer=C1093826151&age=4&gender=M&zipcodeOri=28007&merchant=M348934600&zipMerchant=28007&category=es_transportation&amount=4.55 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.52.1
> Accept: */*
> 
< HTTP/1.1 200 
< Content-Type: application/json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Sun, 12 Apr 2020 19:30:05 GMT
< 
* Curl_http_done: called premature == 0
* Connection #0 to host localhost left intact
{"fraud.1":"0.01028668978375894","fraud.0":"0.9897133102162411"}
```

and apachebench for simple load-testing:

```
ab -k -c 40 -n 100000 "http://localhost:8080/predict_parameters?field1=value1&field2=value2"
```

output:

```
This is ApacheBench, Version 2.3 <$Revision: 1706008 $>
Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
Licensed to The Apache Software Foundation, http://www.apache.org/

Benchmarking localhost (be patient)
Completed 10000 requests
Completed 20000 requests
Completed 30000 requests
Completed 40000 requests
Completed 50000 requests
Completed 60000 requests
Completed 70000 requests
Completed 80000 requests
Completed 90000 requests
Completed 100000 requests
Finished 100000 requests


Server Software:        
Server Hostname:        localhost
Server Port:            8080

Document Path:          /predict_parameters?step=0&customer=C1093826151&age=4&gender=M&zipcodeOri=28007&merchant=M348934600&zipMerchant=28007&category=es_transportation&amount=4.55
Document Length:        64 bytes

Concurrency Level:      40
Time taken for tests:   6.540 seconds
Complete requests:      100000
Failed requests:        0
Keep-Alive requests:    0
Total transferred:      18300000 bytes
HTML transferred:       6400000 bytes
Requests per second:    15290.97 [#/sec] (mean)
Time per request:       2.616 [ms] (mean)
Time per request:       0.065 [ms] (mean, across all concurrent requests)
Transfer rate:          2732.66 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   0.2      1       2
Processing:     1    2   1.1      2      31
Waiting:        1    2   1.1      2      30
Total:          1    3   1.1      3      32

Percentage of the requests served within a certain time (ms)
  50%      3
  66%      3
  75%      3
  80%      3
  90%      3
  95%      3
  98%      3
  99%      4
 100%     32 (longest request)
```