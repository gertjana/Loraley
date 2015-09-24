# UdpStreamingTest

Test project that uses
 - Akka Http (UDP / HTTP connection)
 - Akka Reactive Streams (parse/transform udp packets)
 - Akka Persistence or Hazelcast (store package information)
 
to start
 - optionally configure address/ports in application.conf
 - `sbt run` or
 - `sbt assembly` and then `java -jar target/scala_2.11/udp-streaming-test.jar`

run as a cluster
 - create multiple fat jars using `sbt assembly` with in each an unique application.conf
  - enable udp on only one
  - make sure http and hazelcast ports are unique 
  - start all with `java -jar name-of.jar`
 
