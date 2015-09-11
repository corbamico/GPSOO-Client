package com.hippo.gps

import akka.actor._
import akka.util._
import org.scala_tools.time.Imports._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

//import java.io._

object GPSOOClientApp {

    val ACCOUNT = "username"
    val PASSWORD = "password"
    val IMEI = "861234567890"
    val FILENAME_SAVE = "track.txt"

    val END_TIME   = DateTime.now
    val BEGIN_TIME = END_TIME - 1.day
    

    def main(args: Array[String]): Unit = {
        import akka.pattern.ask
        implicit val timeout = Timeout(2 *1000)

        val _system = ActorSystem("GPSOOClientApp")
        val supervisorActor = _system.actorOf(Props[SupervisorActor],"supervisorActor")

        supervisorActor ! COMMAND_START(ACCOUNT,PASSWORD,IMEI,BEGIN_TIME,END_TIME)
        
        val future = supervisorActor ask COMMAND_STOP()
        val result = Await.result(future,timeout.duration)
        println(result)

        _system.shutdown
    }
}