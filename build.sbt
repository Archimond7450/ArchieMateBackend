lazy val akkaHttpVersion = "10.6.0"
lazy val akkaVersion = "2.9.1"
lazy val postgresVersion = "42.7.3"
lazy val swaggerVersion = "2.2.21"
lazy val slickVersion = "3.5.1"
lazy val circeVersion = "0.14.9"

ThisBuild / scalaVersion      := "2.13.13"
ThisBuild / version           := "0.0.1.SNAPSHOT"
ThisBuild / organization      := "com.archimond7450"
ThisBuild / organizationName  := "Archimond7450"

resolvers += "Akka library repository" at "https://repo.akka.io/maven"

assemblyMergeStrategy in assembly := {
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x => val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


lazy val root = (project in file("."))
  .settings(
    name := "ArchieMate",
    libraryDependencies ++= Seq(
      // Akka
      "com.typesafe.akka"             %%  "akka-actor"                  % akkaVersion,
      "com.typesafe.akka"             %%  "akka-persistence"            % akkaVersion,
      "com.typesafe.akka"             %%  "akka-stream"                 % akkaVersion,
      "com.typesafe.akka"             %%  "akka-http"                   % akkaHttpVersion,


      // JDBC with PostgreSQL
      "org.postgresql"                %   "postgresql"                  % postgresVersion,
      "com.lightbend.akka"            %%  "akka-persistence-jdbc"       % "5.4.0"           exclude("com.typesafe.akka", "akka-persistence-query"),

      // Circe
      "io.circe"                      %%  "circe-core"                  % circeVersion,
      "io.circe"                      %%  "circe-generic"               % circeVersion,
      "io.circe"                      %%  "circe-generic-extras"        % "0.14.4",
      "io.circe"                      %%  "circe-parser"                % circeVersion,
      "de.heikoseeberger"             %%  "akka-http-circe"             % "1.39.2",

      // Swagger

      // Scala-Logging
      "ch.qos.logback"                %   "logback-classic"             % "1.5.6",
      "com.typesafe.akka"             %%  "akka-slf4j"                  % "2.8.5",
      "org.codehaus.janino"           %   "janino"                      % "3.1.12",

      // tests
      "com.typesafe.akka"             %%  "akka-http-testkit"           % akkaHttpVersion % Test,
      "com.typesafe.akka"             %%  "akka-actor-testkit-typed"    % akkaVersion % Test,
      "com.typesafe.akka"             %%  "akka-persistence-testkit"    % akkaVersion % Test,
      "org.scalatest"                 %%  "scalatest"                   % "3.2.19" % Test,
      "org.mockito"                   %%  "mockito-scala"               % "1.17.37" % Test,
    )
  )
