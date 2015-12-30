name         := "Loraley"

organization := "org.thethingsnetwork"

version      := "1.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= {
  val akkaV             = "2.4.1"
  val akkaStreamV       = "2.0"
  val scalaTestV        = "2.2.5"
  Seq(
    "com.typesafe.akka"           %% "akka-actor"                           % akkaV,
    "com.typesafe.akka"           %% "akka-slf4j"                           % akkaV,
    "com.typesafe.akka"           %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka"           %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka"           %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka"           %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka"           %% "akka-http-testkit-experimental"       % akkaStreamV,
    "joda-time"                   % "joda-time"                             % "2.6",
    "org.joda"                    % "joda-convert"                          % "1.7",
    "com.hazelcast"               % "hazelcast"                             % "3.2",
    "com.hazelcast"               % "hazelcast-client"                      % "3.2",
    "org.scalatest"               %% "scalatest"                            % scalaTestV % "test",
    "org.slf4s"                   %% "slf4s-api"                            % "1.7.12",
    "ch.qos.logback"              % "logback-classic"                       % "1.0.0" % "runtime"
  )
}

fork := true

enablePlugins(JavaAppPackaging)

import NativePackagerHelper._

mappings in Universal ++= directory("conf")

mainClass in Compile := Some("boot.Main")
