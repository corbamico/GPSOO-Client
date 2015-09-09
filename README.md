# GPSOO-Client#

## Overview ##

(gpsoo)[www.gpsoo.net] is Platform for car tracking, powerby Goome.
  
GPSOO Client is project writen by Scala, invoke (DataAPI)[http://www.gpsoo.net/open/v1.0/dataApi.html] to backup all your tracking in server.

The data in server only be stored for 150 days, *GPSOO Client* can download all GPS info in your local disk for safely reason.

## Project Description ##

There is two version of GPSOO Client
* (GPSOOClient_Sync) Using Blocking Scalaj.HTTP
* (GPSOOClient_Async) Using akka.actor method (not yet, I plan to)

## Compile&Run ##

    >cd GPSOOClient_Sync
    >sbt compile
    >sbt run

   




