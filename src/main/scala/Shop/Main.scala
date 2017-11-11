package Shop

import akka.actor.{ActorSystem, Props}

/**
  * Created by Dominik on 30.10.2017.
  */

object MainShopApp extends App{
  val system = ActorSystem("app")
  val customer1 = system.actorOf(Props[Customer], "customer1")

  customer1 ! Customer.Init
}