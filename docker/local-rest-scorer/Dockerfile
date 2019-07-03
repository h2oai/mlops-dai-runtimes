FROM openjdk:8-jre-alpine

ENV DRIVERLESS_AI_LICENSE_FILE /root/license.sig

COPY build/local-rest-scorer.jar /opt/h2oai/rest-scorer/

EXPOSE 8080

ENTRYPOINT ["java", "-Dmojo.path=/mojos/pipeline.mojo", "-jar", "/opt/h2oai/rest-scorer/local-rest-scorer.jar"]
