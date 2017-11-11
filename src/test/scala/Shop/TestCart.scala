package Shop

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpec, WordSpecLike}

/**
  * Created by Dominik on 30.10.2017.
  */

class TestCart extends TestKit(ActorSystem("TestCart"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {
  override def afterAll(): Unit = system.terminate

  "[Sync] A Cart " must {
    "Be empty on init " in {
      val testCart = TestActorRef[Cart]
      testCart ! Cart.Init
      assert(testCart.underlyingActor.itemsNum == 0)
    }
    "Add / remove products " in {
      val testCart = TestActorRef[Cart]
      testCart ! Cart.Init
      testCart ! Cart.AddItem
      assert(testCart.underlyingActor.itemsNum == 1)
      testCart ! Cart.AddItem
      assert(testCart.underlyingActor.itemsNum == 2)
      testCart ! Cart.AddItem
      assert(testCart.underlyingActor.itemsNum == 3)
      testCart ! Cart.ItemRemoved
      assert(testCart.underlyingActor.itemsNum == 2)
      testCart ! Cart.ItemRemoved
      assert(testCart.underlyingActor.itemsNum == 1)
      testCart ! Cart.ItemRemoved
      assert(testCart.underlyingActor.itemsNum == 0)
      testCart ! Cart.AddItem
      assert(testCart.underlyingActor.itemsNum == 1)
    }
    "Become empty when timeout triggered in nonEmpty state" in {
      val testCart = TestActorRef[Cart]
      testCart ! Cart.Init
      testCart ! Cart.AddItem
      testCart ! Cart.AddItem
      testCart ! Cart.AddItem
      Thread.sleep(3*1000)
      assert(testCart.underlyingActor.itemsNum == 0)
    }
  }

  "[Async] A Cart " must {
    "Process transaction successfully" in {
      val testCart = TestActorRef[Cart]
      testCart ! Cart.Init
      testCart ! Cart.AddItem
      testCart ! Cart.StartCheckout
      expectMsgType[Cart.CheckoutStarted]
      testCart ! Cart.CheckoutClosed
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