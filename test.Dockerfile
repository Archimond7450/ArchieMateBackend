FROM sbtscala/scala-sbt:eclipse-temurin-alpine-22_36_1.10.1_3.4.2

WORKDIR /usr/src/app

COPY . ./

CMD ["sbt", "test"]