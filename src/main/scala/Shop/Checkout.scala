package Shop

import akka.actor.{Actor, ActorRef, ActorSystem, LoggingFSM, Timers, Props}
import akka.event.LoggingReceive

import scala.concurrent.duration._

/**
  * Created by Dominik on 30.10.2017.
  */

object Checkout{
  case class Init(productsNum: Int)
  case object CheckoutStarted
  case object Cancelled
  case object CheckoutTickKey
  case object CheckoutTimeOutMsg
  case object CheckoutTimerExpired
  case object PaymentTickKey
  case object PaymentTimeOutMsg
  case object PaymentTimerExpired
  case object DeliveryMethodSelected
  case object PaymentSelected
  case object PaymentReceived
  case class PaymentServiceStarted(paymentServiceRef: ActorRef)
}
class Checkout extends Actor with Timers {
  val system = ActorSystem("checkoutSys")

  override def receive: Receive = LoggingReceive{
    case Checkout.Init(productsNum) if productsNum > 0   =>
      timers.startSingleTimer(Checkout.CheckoutTickKey, Checkout.CheckoutTimeOutMsg, 5.seconds)
      context become selectingDelivery()
    case Checkout.Init(productsNum) if productsNum == 0  =>{
      println("empty cart")
      context become closed()
    }

  }
  def selectingDelivery(): Receive = LoggingReceive{
    case Checkout.Cancelled =>
      context become cancelled()
    case Checkout.CheckoutTimerExpired =>
      context become cancelled()
    case Checkout.DeliveryMethodSelected =>
      context become selectingPaymentMethod()
    case Checkout.CheckoutTimeOutMsg =>{
      println("checkout timeout")
      context become cancelled()
    }
  }
  def cancelled(): Receive = LoggingReceive{
    case _ =>
      println("Checkout cancelled")
  }
  def selectingPaymentMethod(): Receive = LoggingReceive{
    case Checkout.Cancelled =>
      context become cancelled()
    case Checkout.CheckoutTimerExpired =>
      context become cancelled()
    case Checkout.PaymentSelected =>
      val customerRef = sender()
      val paymentServiceRef: ActorRef = system.actorOf(Props[PaymentService], "paymentService1")
      sender() ! Checkout.PaymentServiceStarted(paymentServiceRef)
      timers.startSingleTimer(Checkout.PaymentTickKey, Checkout.PaymentTimeOutMsg, 5.seconds)
      context become processingPayment(customerRef)
    case Checkout.CheckoutTimeOutMsg =>{
      println("checkout timeout")
      context become cancelled()
    }
  }
  def processingPayment(customerRef: ActorRef): Receive = LoggingReceive{
    case Checkout.Cancelled =>
      context become cancelled()
    case Checkout.PaymentTimerExpired =>
      context become cancelled()
    case Checkout.PaymentReceived => {
      customerRef ! Cart.CheckoutClosed
      context.parent ! Cart.CheckoutClosed
      context become closed()
    }
    case Checkout.PaymentTimeOutMsg =>{
      println("payment timeout")
      context become cancelled()
    }
  }
  def closed(): Receive = LoggingReceive{
    case _ =>
      println("[checkout closed]")
  }
}