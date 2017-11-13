package Shop

import akka.actor.{ActorRef}

/**
  * Created by Dominik on 30.10.2017.
  */

object Cart {
  case object AddItem
  case object RemoveItem
  case object StartCheckout
  case class CheckoutStarted(checkoutRef: ActorRef)
  case object CheckoutCancelled
  case object CheckoutClosed
  case object TickKey
  case object TimeOutMsg
  case object CartEmpty
  case object ItemAdded
  case object ItemRemoved
}

//states
sealed trait CartState
case object Empty extends CartState
case object NotEmpty extends CartState
case object InCheckout extends CartState

case class CartStateChangeEvent(state: CartState)
case class ItemsBalanceChangeEvent(delta: Int)

case class Cart(itemsNum: Int = 0) {
  def update(evt: ItemsBalanceChangeEvent): Cart = {
    println(s"Applying $evt")
    if (evt.delta != 0)
      Cart(itemsNum + evt.delta)
    else
      Cart()
  }

  override def toString: String = itemsNum.toString
}


