#
# Build stage
#
FROM maven:3.8.6-eclipse-temurin-11-alpine AS build
WORKDIR /app
ADD settings.xml /root/.m2/
COPY pom.xml /app
RUN --mount=type=cache,target=/root/.m2,rw mvn dependency:go-offline -B
COPY src ./src
RUN --mount=type=cache,target=/root/.m2,rw mvn -Dmaven.test.skip=true clean package

#
# Package stage
#
FROM eclipse-temurin:11.0.17_8-jre
COPY newrelic /usr/local/lib/newrelic
COPY --from=build /app/target/stream-processor-0.0.1-SNAPSHOT.jar /usr/local/lib/stream-processor.jar
VOLUME /app/state
EXPOSE 8992
ENTRYPOINT ["sh", "-c", "java -Dnewrelic.environment=$ENV -javaagent:/usr/local/lib/newrelic/newrelic.jar -jar /usr/local/lib/stream-processor.jar" ]

