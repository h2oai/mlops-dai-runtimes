FROM cgr.dev/chainguard/wolfi-base:latest@sha256:ef6dd240997674c8a940dd9ab565dd3e8700b8f7a8e7b743ed16b925d81a70ef AS builder
RUN apk add openjdk-17 bash coreutils
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"
WORKDIR /app
COPY build/libs/local-rest-scorer-boot.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM cgr.dev/chainguard/wolfi-base:latest@sha256:ef6dd240997674c8a940dd9ab565dd3e8700b8f7a8e7b743ed16b925d81a70ef
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
