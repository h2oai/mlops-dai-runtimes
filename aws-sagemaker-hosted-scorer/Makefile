default: build

clean:
	./gradlew clean

build_java:
	./gradlew build

build_java_in_docker:
	docker build \
		-t opsh2oai/build-aws-sagemaker-hosted-scorer \
		-f Dockerfile-build \
		.
	docker run \
		--rm \
		--init \
		-u `id -u`:`id -g` \
		-v `pwd`:/dot \
		-w /dot \
		-e "HOME=/dot" \
		--entrypoint /bin/bash \
		opsh2oai/build-aws-sagemaker-hosted-scorer \
		-c "make build_java"
	$(MAKE) build_java

build_runtime:
	docker build \
		-t h2oai/dai-sagemaker-hosted-scorer \
		-f Dockerfile-runtime \
		.

build:
	$(MAKE) build_java_in_docker
	$(MAKE) build_runtime

run:
	docker run \
		--rm \
		--init \
		-ti \
		-v `pwd`:/opt/ml/model \
		-e "DRIVERLESS_AI_LICENSE_KEY=$(DRIVERLESS_AI_LICENSE_KEY)" \
		-p 8080:8080 \
		h2oai/dai-sagemaker-hosted-scorer \
		serve

.PHONY: clean build run
