FROM gradle:jdk-alpine AS build
WORKDIR /home/gradle
COPY . .
RUN ./gradlew clean installdist
ENTRYPOINT ["./migrator/build/install/migrator/bin/migrator"]