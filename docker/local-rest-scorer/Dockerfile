FROM ubuntu:18.04

RUN apt-get update \
  && apt-get install -y openjdk-8-jdk \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/

ENV DRIVERLESS_AI_LICENSE_FILE /root/license.sig

COPY assets/local-rest-scorer.jar /opt/h2oai/rest-scorer/

EXPOSE 8080

ENTRYPOINT ["java", "-Dmojo.path=/mojos/pipeline.mojo", "-jar", "/opt/h2oai/rest-scorer/local-rest-scorer.jar"]
