package com.hippo.gps


import akka.actor._
import java.security.MessageDigest
import java.io._
import org.scala_tools.time.Imports._
import org.json4s._
import org.json4s.native.JsonMethods._
import scalaj.http._

case class GPSPoint(gps_time:String,lng:Double,lat:Double,course:Int,speed:Int) {
    override def toString = s"$gps_time,$lng,$lat,$course,$speed"
}

trait CommandMessage
case class COMMAND_START(username:String,password:String,imei:String,begin_time:DateTime,end_time:DateTime) extends CommandMessage
case class COMMAND_STOP  extends CommandMessage
case class GPSPointsMsg(points:List[GPSPoint]) extends CommandMessage


abstract class LinkedActor(nextActor:ActorRef) extends Actor{
    def defaultReceive:Receive = {
        case msg:COMMAND_START => nextActor ! msg
        case msg:COMMAND_STOP  => nextActor forward msg
    } 
}

class SupervisorActor extends Actor{
    val serializeActor = context.actorOf(Props(new SerializeActor(null)),"serializeActor")

    val httpActor = context.actorOf(Props(new HttpActor(serializeActor)),"httpActor")

    def receive = {
        case msg:COMMAND_START => httpActor!msg
        case msg:COMMAND_STOP  => httpActor forward msg
    }
}
class HttpActor(nextActor:ActorRef) extends LinkedActor(nextActor){
    val client = GPSOOClient()

    def receive = {
        case COMMAND_START(username,password,imei,begin_time,end_time) => 
             client.login(username,password)
                   .getHistory(imei,begin_time,end_time){
                        p => nextActor ! GPSPointsMsg(p)
                      }

        case msg:COMMAND_STOP => 
            if (client.getMessage != "") sender ! client.getMessage else nextActor forward msg
    }
}



class SerializeActor(nextActor:ActorRef = null) extends LinkedActor(nextActor){
    def receive = {
        case msg:COMMAND_STOP => sender ! msg
        case GPSPointsMsg(p) => {println("save points");println(p.mkString("\n"))}
    }
}


case class GPSOOClient{
    private val MAX_RECORDS_LIMIT = 500

    //if access_token=="", then consider as not login, or access_token expired
    private var access_token = ""
    private var username = ""

    var error_message = ""
    def getMessage = error_message
    //private  var isLogin = true


    implicit class UnixTimestamp(dt:DateTime){
        def asUnixTimestamp:String = (dt.getMillis/1000).toString
    }
    

    def md5(s:String):String = {
        MessageDigest.getInstance("MD5").digest(s.getBytes).map("%02x".format(_)).mkString
    }
    
    def login(username:String,password:String) = {
        val url = "http://api.gpsoo.net/1/auth/access_token?"
        
        val ts = DateTime.now.asUnixTimestamp
        val signature = md5(md5(password) + ts)
        this.username = username
        
        //md5(md5(password of user) + time)
        val data = Seq("account"->username,"time"->ts,"signature"->signature)
        
        try {
            val res:String = Http(url).postForm(data).asString.body
            access_token = compact(render((parse(res) \ "access_token")))

        }catch{
            case e:Exception => 
                error_message =  e.getClass.getName + ":" + e.getMessage 
                //println(error_message)
        }
        //if login success   
        println(access_token)
        this
    }  

    def getHistory(imei:String,fromTime:DateTime,toTime:DateTime)(implicit f:List[GPSPoint]=>Unit ) : Unit = {
         
         //fromTime toTime less than 30 days
        def getHistoryInternal(fromTime:DateTime,toTime:DateTime)(implicit f:List[GPSPoint]=>Unit){
            implicit val Formats = DefaultFormats

            var size = 0
            var lastTime:String = "0"
            val url = "http://api.gpsoo.net/1/devices/history?"
            val data = Seq[(String,String)]("account"->username,
            "access_token"->access_token,
            "time"->DateTime.now.asUnixTimestamp,
            "imei"->imei,
            "map_type"->"BAIDU",
            "begin_time"-> fromTime.asUnixTimestamp,
            "end_time"-> toTime.asUnixTimestamp,
            "limit"->s"$MAX_RECORDS_LIMIT"
            )

            try { 
                val json = parse(Http(url).postForm(data).asString.body)

                //println(data)
                //println(json)

                (json \ "ret").extract[Int] match {
                    case 0 => 
                        val points:List[GPSPoint] = (json \ "data").extract[List[GPSPoint]]
                        size = points.size
                        if (size == MAX_RECORDS_LIMIT) lastTime = points.last.gps_time 
                        f(points)

                    case 10006 => access_token="" //access_token已过期,请重新获取
                    case _ => println 
                }
            } 
            catch {
              case e: Exception => 
                error_message = e.getClass.getName + ":" + e.getMessage 
                //println(error_message)
            }

                    
            /*
            Http(url).postForm(data).execute(parser={
                inputStream=>
                val points:List[GPSPoint] = (parse(inputStream) \ "data").extract[List[GPSPoint]]
                size = points.size
                lastTime = points.last.gps_time
                points.map(f)
                })
            */

            //if get MAX_RECORDS_LIMIT, then get more records from gps_time of last record to toTime 
            if (size == MAX_RECORDS_LIMIT) getHistoryInternal(new DateTime( lastTime.toLong * 1000),toTime)(f)
        }

        if (access_token==""||(fromTime>=toTime)||(fromTime < DateTime.now - 150.day)) {
            //wrong parameter,do nothing
            //println("do nothing in getHistory")
        } else if ((fromTime + 30.day) >= toTime) 
            getHistoryInternal(fromTime,toTime)(f)
        else{
            getHistoryInternal(fromTime,fromTime+30.day)(f)
            getHistory(imei,fromTime+30.day,toTime)
        }

    }    


}
