package Shop

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec, WordSpecLike}
import scala.concurrent.duration._
/**
  * Created by Dominik on 30.10.2017.
  */


/*
  NOTE: TESTS SHOULD BE RUN IN MEMORY
  set inmemory journal, snapshot in application.conf
 */

class TestCart extends TestKit(ActorSystem("TestCart"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender with Matchers{
  override def afterAll(): Unit = system.terminate

    "A Cart" must {
      "Be empty on init " in {
        val testCart = Cart()
        assert(testCart.itemsNum == 0)
      }
      "Add / remove products " in {
        var testCart = Cart()
        val eventAdd = ItemsBalanceChangeEvent(1)
        val eventRemove = ItemsBalanceChangeEvent(-1)
        testCart = testCart.update(eventAdd)
        assert(testCart.itemsNum == 1)
        testCart = testCart.update(eventAdd)
        assert(testCart.itemsNum == 2)
        testCart = testCart.update(eventRemove)
        assert(testCart.itemsNum == 1)
        testCart = testCart.update(eventRemove)
        assert(testCart.itemsNum == 0)
      }
    }


    "CartManager" must {

      "Process transaction successfully with no interrupts" in {
      val cartManagerId = "test-id-0009991"
      val cartManager = system.actorOf(Props(new CartManager(cartManagerId)), "cart-test-01")
      cartManager ! Cart.AddItem
      expectMsg(Cart.ItemAdded)
      cartManager ! Cart.StartCheckout
      expectMsgType[Cart.CheckoutStarted]
      cartManager ! Cart.CheckoutClosed
      expectMsg(Cart.CartEmpty)
    }

    "Transaction is not continued after kill" in {
      val cartManagerId = "test-id-0009992"
      val cartManager = system.actorOf(Props(new CartManager(cartManagerId)), "cart-test-02")
      cartManager ! Cart.AddItem
      expectMsg(Cart.ItemAdded)

      cartManager ! PoisonPill

      cartManager ! Cart.AddItem
      expectNoMessage(1.seconds)
    }

    "Continue transaction after restart" in {
      val cartManagerId = "test-id-0009993"
      val cartManager1 = system.actorOf(Props(new CartManager(cartManagerId)), "cart-test-03")
      cartManager1 ! Cart.AddItem
      expectMsg(Cart.ItemAdded)
      cartManager1 ! Cart.StartCheckout
      expectMsgType[Cart.CheckoutStarted]

      cartManager1 ! PoisonPill
      val cartManager2 = system.actorOf(Props(new CartManager(cartManagerId)), "cart-test-04")

      cartManager2 ! Cart.CheckoutClosed
      expectMsg(Cart.CartEmpty)
    }
  }


  "A checkout " must {
    "send ack to parent" in {
        val cartParent = TestProbe()
        val checkoutChild = cartParent.childActorOf(Props[Checkout], "checkout1")
        checkoutChild ! Checkout.Init(10)
        checkoutChild ! Checkout.DeliveryMethodSelected
        checkoutChild ! Checkout.PaymentSelected
        expectMsgType[Checkout.PaymentServiceStarted]
        checkoutChild ! Checkout.PaymentReceived
        cartParent.expectMsg(Cart.CheckoutClosed)
    }
  }

}