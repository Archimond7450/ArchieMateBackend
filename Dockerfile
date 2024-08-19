FROM sbtscala/scala-sbt:eclipse-temurin-alpine-22_36_1.10.1_3.4.2 AS build

WORKDIR /usr/src/app

COPY . ./

RUN sbt assembly

FROM docker.io/library/eclipse-temurin:22.0.2_9-jre-alpine AS app

WORKDIR /usr/src/app

RUN apk update && apk add --no-cache curl

COPY --from=build /usr/src/app/target/scala-2.13/ArchieMate-assembly-0.0.1.SNAPSHOT.jar ./ArchieMate.jar
COPY ./src/main/resources/application.conf ./src/main/resources/logback.xml ./

CMD ["java", "-jar", "./ArchieMate.jar", "-Dconfig.file=application.conf", "-Dlogback.configurationFile=logback.xml"]
