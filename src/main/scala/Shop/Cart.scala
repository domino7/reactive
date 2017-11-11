package Shop

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Timers}
import akka.event.LoggingReceive

import scala.concurrent.duration._


/**
  * Created by Dominik on 30.10.2017.
  */

object Cart {
  case object Init
  case object AddItem
  case object ItemRemoved
  case object StartCheckout
  case class CheckoutStarted(checkoutRef: ActorRef)
  case object CheckoutCancelled
  case object CheckoutClosed
  case object TickKey
  case object TimeOutMsg
  case object CartEmpty
}
class Cart extends Actor with Timers {
  val system = ActorSystem("cartSys")
  var itemsNum = 0
  val checkout1: ActorRef = system.actorOf(Props[Checkout], "checkout1")

  override def receive: Receive = LoggingReceive{
    case Cart.Init => {
      printItems()
      context become empty()
    }
  }
  def empty(): Receive = LoggingReceive{
    case Cart.AddItem =>
      itemsNum += 1
      timers.startSingleTimer(Cart.TickKey, Cart.TimeOutMsg, 2.seconds)
      context become nonEmpty()
  }
  def nonEmpty(): Receive = LoggingReceive{
    case Cart.AddItem =>
      itemsNum += 1
    case Cart.ItemRemoved if itemsNum == 1 =>
      itemsNum = 0
      context become empty()
    case Cart.ItemRemoved if itemsNum > 1 =>
      itemsNum -= 1
    case Cart.StartCheckout =>
      val customerRef = sender

      customerRef ! Cart.CheckoutStarted(checkout1)
      context become inCheckout(customerRef)
    case Cart.TimeOutMsg =>
      println("timeout")
      itemsNum = 0
      context become empty()
  }
  def inCheckout(customerRef: ActorRef): Receive = LoggingReceive{
    case Cart.CheckoutCancelled =>
      timers.startSingleTimer(Cart.TickKey, Cart.TimeOutMsg, 10.seconds)
      context become nonEmpty()
    case Cart.CheckoutClosed =>
      println("checkoutClosed")
      customerRef ! Cart.CartEmpty
      context become empty()
  }
  def startTimer(): Unit = {}
  def printItems(): Unit = { println("items:" + itemsNum)}
}