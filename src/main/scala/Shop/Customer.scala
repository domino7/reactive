package Shop

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.LoggingReceive

/**
  * Created by Dominik on 30.10.2017.
  */

object Customer{
  case object Init
}

class Customer extends Actor{
  val cart1 = context.actorOf(Props(new CartManager("CartManager0001-run")), "cart1")

  override def receive: Receive = LoggingReceive{
    case Customer.Init => {
      cart1 ! Cart.AddItem
      cart1 ! Cart.StartCheckout
      context become waitingForCheckout()
    }
  }
  def waitingForCheckout(): Receive = LoggingReceive{
    case Cart.CheckoutStarted(checkoutRef) => {
      val cartRef = sender()
      checkoutRef ! Checkout.Init(1)
      checkoutRef ! Checkout.DeliveryMethodSelected
      checkoutRef ! Checkout.PaymentSelected
      context become inPaymentService(checkoutRef)
    }
  }
  def inPaymentService(checkoutRef: ActorRef): Receive = LoggingReceive{
    case Checkout.PaymentServiceStarted(paymentServiceRef) => {
      paymentServiceRef ! PaymentService.DoPayment(checkoutRef)
    }
    case PaymentService.PaymentConfirmed => {
      println("payment confirmed")
      context become waitingForCheckOutClosed()
    }
  }
  def waitingForCheckOutClosed(): Receive = LoggingReceive{
    case Cart.CheckoutClosed => {
      println("customer - checkout closed")
      context become emptingCart()
    }
  }
  def emptingCart(): Receive = LoggingReceive{
    case  Cart.CartEmpty => {
      println("SHOPPING FINISHED")
    }
  }
}