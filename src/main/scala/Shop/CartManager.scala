package Shop

import akka.actor.{ActorLogging, ActorRef, ActorSystem, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import scala.concurrent.duration._
/**
  * Created by Dominik on 13.11.2017.
  */


class CartManager(id: String) extends PersistentActor with Timers with ActorLogging {
  val system = ActorSystem("cartSys")
  override def persistenceId: String = "CartManager0001"

  var state = Cart()
  def updateCartItems(event: ItemsBalanceChangeEvent): Unit = {
    state = state.update(event)
  }
  def updateState(event: CartStateChangeEvent): Unit =
    context.become(
      event.state match {
        case Empty => empty
        case NotEmpty => nonEmpty
        case InCheckout => inCheckout
      }
    )

  val checkout1: ActorRef = system.actorOf(Props[Checkout], "checkout1")

  def empty(): Receive = LoggingReceive{
    case Cart.AddItem =>{
      persist(ItemsBalanceChangeEvent(1)){
        sender ! Cart.ItemAdded
        event => updateCartItems(event)
      }
      persist(CartStateChangeEvent(NotEmpty)){
        event =>
          timers.startSingleTimer(Cart.TickKey, Cart.TimeOutMsg, 2.seconds)
          updateState(event)
      }
    }
  }

  def nonEmpty(): Receive = LoggingReceive{
    case Cart.AddItem =>{
      persist(ItemsBalanceChangeEvent(1)){
        sender ! Cart.ItemAdded
        event => updateCartItems(event)
      }
    }
    case Cart.RemoveItem if state.itemsNum == 1 =>{
      persist(ItemsBalanceChangeEvent(-1)) {
        sender ! Cart.ItemRemoved
        event => updateCartItems(event)
      }
      persist(CartStateChangeEvent(Empty)){
        event => updateState(event)
      }
    }
    case Cart.RemoveItem if state.itemsNum > 1 => {
      persist(ItemsBalanceChangeEvent(-1)) {
        sender ! Cart.ItemAdded
        event => updateCartItems(event)
      }
    }
    case Cart.StartCheckout =>{
      persist(CartStateChangeEvent(InCheckout)){
        event => updateState(event)
          sender ! Cart.CheckoutStarted(checkout1)
      }
    }

    case Cart.TimeOutMsg =>{
      log.info("timeout")
      persist(ItemsBalanceChangeEvent(0)) {
        event => updateCartItems(event)
      }
      persist(CartStateChangeEvent(Empty)){
        event => updateState(event)
      }
    }
  }

  def inCheckout(): Receive = LoggingReceive{
    case Cart.CheckoutCancelled =>{
      persist(CartStateChangeEvent(NotEmpty)){
        event =>
          timers.startSingleTimer(Cart.TickKey, Cart.TimeOutMsg, 10.seconds)
          updateState(event)
      }
    }

    case Cart.CheckoutClosed =>{
      log.info("checkoutClosed")
      persist(ItemsBalanceChangeEvent(0)) {
        event => updateCartItems(event)
      }
      persist(CartStateChangeEvent(Empty)){
        sender ! Cart.CartEmpty
        event => updateState(event)
      }
    }
  }
  //  def startTimer(): Unit = {}
  //  def printItems(): Unit = { println("items:" + state.itemsNum)}

  override def receiveRecover: Receive = {
    case evt: ItemsBalanceChangeEvent => updateCartItems(evt)
    case evt: CartStateChangeEvent => updateState(evt)
    case SnapshotOffer(_, snapshot: Cart) => state = snapshot
    case RecoveryCompleted => log.info("Recovery completed!")
  }

  override def receiveCommand: Receive = empty
}