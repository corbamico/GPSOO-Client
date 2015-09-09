package com.hippo.gps

import org.scala_tools.time.Imports._
import java.io._

object GPSOOClientApp {

    val ACCOUNT = "username"
    val PASSWORD = "password"
    val IMEI = "861234567890"
    val FILENAME_SAVE = "track.txt"

    val END_TIME   = DateTime.now
    val BEGIN_TIME = END_TIME - 2.day
    

    def main(args: Array[String]): Unit = {
        val client = GPSOOClient(ACCOUNT,PASSWORD)
        val now = DateTime.now
        val outputStream = new FileOutputStream(FILENAME_SAVE)

        client.login
              .getHistory(IMEI, BEGIN_TIME, END_TIME){
                 p => 
                   outputStream.write(p.toString.getBytes)
                   outputStream.write('\n')
              }

        println(client.getMessage)

        outputStream.close()        
    }
}