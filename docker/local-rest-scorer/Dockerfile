FROM ubuntu:18.04

RUN apt-get update \
  && apt-get install -y openjdk-8-jdk \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/

ENV DRIVERLESS_AI_LICENSE_FILE /root/license.sig

COPY assets/local-rest-scorer.jar \
     assets/start-mojo-scorer.sh /opt/h2oai/mojo-scorer/

RUN chmod +x /opt/h2oai/mojo-scorer/start-mojo-scorer.sh

EXPOSE 8080

ENTRYPOINT ["/opt/h2oai/mojo-scorer/start-mojo-scorer.sh"]
