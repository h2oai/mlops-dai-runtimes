FROM cgr.dev/chainguard/wolfi-base:latest@sha256:0f1d81605bda6e2388c3c7f731700d8c12e17259d58ffba11f36ddc81d9c0a76 AS builder
RUN apk add openjdk-17 bash coreutils
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"
WORKDIR /app
COPY build/libs/local-rest-scorer-0.0.0-boot.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM cgr.dev/chainguard/wolfi-base:latest@sha256:0f1d81605bda6e2388c3c7f731700d8c12e17259d58ffba11f36ddc81d9c0a76
RUN apk add openjdk-17-jre bash coreutils
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"
USER nonroot
WORKDIR /app
COPY --from=builder --chown=nonroot:nonroot /app/dependencies/ ./
COPY --from=builder --chown=nonroot:nonroot /app/spring-boot-loader/ ./
COPY --from=builder --chown=nonroot:nonroot /app/snapshot-dependencies/ ./
COPY --from=builder --chown=nonroot:nonroot /app/application/ ./
VOLUME /mojos
VOLUME /secrets
EXPOSE 8080
ENV DRIVERLESS_AI_LICENSE_FILE="/secrets/license.sig"
CMD ["java", "org.springframework.boot.loader.launch.JarLauncher"]
