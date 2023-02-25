# syntax=docker/dockerfile:1
FROM eclipse-temurin:17 as jre-build

# Create a custom Java runtime
RUN JAVA_TOOL_OPTIONS="-Djdk.lang.Process.launchMechanism=vfork" $JAVA_HOME/bin/jlink \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

# Create our application distribution without running tests
FROM ubuntu:latest as app-build
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

COPY gradle/ /opt/app/gradle/
COPY gradlew settings.gradle /opt/app/
COPY migrator/ /opt/app/migrator/

WORKDIR /opt/app
RUN ls -alh . && ./gradlew distTar -x test --no-daemon

FROM ubuntu:latest
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME
COPY --from=app-build /opt/app/migrator/build/distributions/migrator.tar /opt/
RUN tar -xvf /opt/migrator.tar -C /opt/ && rm /opt/migrator.tar
WORKDIR /opt/migrator

ENTRYPOINT ["/opt/migrator/bin/migrator"]
