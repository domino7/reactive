package Shop

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive

/**
  * Created by Dominik on 30.10.2017.
  */

object PaymentService{
  case class DoPayment(checkoutRef: ActorRef)
  case object PaymentConfirmed
}

class PaymentService extends Actor{
  override def receive: Receive = LoggingReceive{
    case PaymentService.DoPayment(checkoutRef) => {
      println("Payment in progress...")
      sender() ! PaymentService.PaymentConfirmed // to customer
      checkoutRef ! Checkout.PaymentReceived
    }
  }
}