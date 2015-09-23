import sbtassembly.AssemblyPlugin.autoImport._

name         := "udp-streaming-test"

organization := "net.addictivesoftware"

version      := "1.0"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= {
  val akkaV             = "2.4-M2"
  val akkaPersistenceV  = "2.4-M2"
  val akkaStreamV       = "1.0"
  val scalaTestV        = "2.2.5"
  Seq(
    "com.typesafe.akka"           %% "akka-actor"                           % akkaV,
    "com.typesafe.akka"           %% "akka-slf4j"                           % akkaV,
    "com.typesafe.akka"           %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka"           %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka"           %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka"           %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka"           %% "akka-http-testkit-experimental"       % akkaStreamV,
    "com.typesafe.akka"           %% "akka-persistence-experimental"        % akkaPersistenceV,
    "org.iq80.leveldb"            % "leveldb"                               % "0.7",
    "org.fusesource.leveldbjni"   % "leveldbjni-all"                        % "1.8",
    "com.hazelcast"               % "hazelcast"                             % "3.2",
    "com.hazelcast"               % "hazelcast-client"                      % "3.2",
    "org.scalatest"               %% "scalatest"                            % scalaTestV % "test",
    "ch.qos.logback"              % "logback-classic"                       % "1.0.0" % "runtime"
  )
}

fork := true

val mainClazz = Some("boot.Main")

mainClass in (Compile, run) := mainClazz

mainClass in assembly := mainClazz

test in assembly := {}

assemblyJarName in assembly := s"udp-streaming-test.jar"