FROM cgr.dev/chainguard/wolfi-base:latest@sha256:8dd9ceace8b1574e550374e9c07c2baafa60cc96223c1314fac61bd2edb48c70 AS builder
RUN apk add openjdk-17 bash coreutils
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"
WORKDIR /app
COPY build/libs/local-rest-scorer-boot.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM cgr.dev/chainguard/wolfi-base:latest@sha256:8dd9ceace8b1574e550374e9c07c2baafa60cc96223c1314fac61bd2edb48c70
RUN apk add openjdk-17-jre bash coreutils
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"
# wolfi-base defines nonroot user under the following uid:gid
USER 65532:65532
WORKDIR /app
COPY --from=builder --chown=nonroot:nonroot /app/dependencies/ ./
COPY --from=builder --chown=nonroot:nonroot /app/spring-boot-loader/ ./
COPY --from=builder --chown=nonroot:nonroot /app/snapshot-dependencies/ ./
COPY --from=builder --chown=nonroot:nonroot /app/application/ ./
VOLUME /mojos
VOLUME /secrets
EXPOSE 8080
ENV DRIVERLESS_AI_LICENSE_FILE="/secrets/license.sig"
CMD ["java", "-Dmojo.path=/mojos/pipeline.mojo", "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED", "org.springframework.boot.loader.launch.JarLauncher"]
