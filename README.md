# Loraley

Test project that uses
 - Akka Http (UDP / HTTP connection)
 - Akka Reactive Streams (parse/transform udp packets)
 - Akka Actor
 - Hazelcast (store package information)
 
Actor Model

Uses hierarchical model 

for instance 2 packets with address 00:11:FF:AA and 00:22:FF:AA wil be stored like:

    RootActor
     \-> 0 (first char of address)
       \-> 0 (second char of address)
         |-> 1 (third char of address)
         |  \-> 1 (package 00:11:FF:AA will be stored here)
         \-> 2 (third char of second address
            \-> 2 (package 00:22:FF:AA will be stored here)
        

to start
 - optionally configure address/ports in application.conf
 - `sbt run` or
 - `sbt universal:packageBin` to create a zip distribution unpack and run `bin/loraley`
 - Alternative configuration can be passed on the commandline, relative to the current directory
   - `sbt "run my.conf"`
   - `bin/loraley my.conf`

run as a cluster
 - create multiple distributions using `sbt universal:packageBin` and put an unique application.conf in each
  - enable udp on only one
  - make sure http and hazelcast ports are unique 
  - start all with `bin/loraley`

 to do 
  - DONE: allow external config to be passed in
  - gateway status package support (Storage+API)
  - duplication detection
  - lifecycle management (remove old/stale data)
  - location awareness (should boston data appear on an amsterdam server?)
