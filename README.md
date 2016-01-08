# Loraley

LoRaWan backend that uses
 - Akka Http (UDP / HTTP connection)
 - scodec (decoding binary protocol)
 - Akka Reactive Streams (parse/transform udp packets)
 - Akka Actors
 - Hazelcast (store package information)
 
## Actor Model

### Uses hierarchical model 

for instance 2 packets with address 00:11:FF:AA and 00:22:FF:AA wil be stored like:

    RootActor
     \-> 0 (first char of address)
       \-> 0 (second char of address)
         |-> 1 (third char of address)
         |  \-> 1 (package 00:11:FF:AA will be stored here)
         \-> 2 (third char of second address
            \-> 2 (package 00:22:FF:AA will be stored here)
        

## To start
 - optionally configure address/ports in your own configuration file
 - `sbt run` or
 - `sbt universal:packageBin` to create a zip distribution unpack and run `bin/loraley`
 - Alternative configuration (samples in conf/) can be passed on the commandline, relative to the current directory
   - `sbt "run conf/my.conf"`
   - `bin/loraley conf/my.conf`

## Run as a cluster
 - create multiple distributions using `sbt universal:packageBin` and add a specific configuration for it (see conf/ for examples)
  - enable udp on only one
  - make sure http and hazelcast ports are unique 
  - start all with `bin/loraley conf/specific.conf`

## Docker
 - optionally add your config to the conf directory
 - execute `docker build -t <tag> .` after `sbt universal:packageBin` to build a docker image
 - run it with `docker run -e conf=conf/your.conf -p 1337:1337 -p 1338:1338\udp --name loraley <tag>`  
  
## Test
  - `sbt test` to run some local tests
  - with `cat samples/packet.json | nc -4u -w1 [host] [port]` you can send some example messages to it `

## To Do 
  - fool proof packet conversion (f.i. handle encrypted messages)
  - gateway status support (Storage+API)
  - duplication detection (packets can come from multiple gateways) 
  - lifecycle management (remove old/stale data)
  - location awareness (should boston data appear on an amsterdam server?)
  - subscribe to events (allow clients to get notified when data from a certain device arrives)
  - Simple UI, display/search/filter on packet/gateway info
  
