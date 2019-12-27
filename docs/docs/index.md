---
layout: home
title:  "Home"
section: "home"
position: 1
---

An error handling library based on Shapeless Coproducts, with a focus on type-safety, readability and ergonomic!

[![Release](https://img.shields.io/nexus/r/com.github.jatcwang/hotpotato-core_2.13?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/releases/com/github/jatcwang/hotpotato-core_2.13/)
[![(https://badges.gitter.im/gitterHQ/gitter.png)](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jatcwang/hotpotato)

# Installation

```
libraryDependencies += "com.github.jatcwang" %% "hotpotato-core" % LATEST_VERSION
```

# Quick Example

```scala mdoc:invisible
import hotpotato._
import zio._
import zio.internal.{Platform, PlatformLive, Tracing}

val zioRuntime: DefaultRuntime = new DefaultRuntime {
  override val platform
  : Platform = PlatformLive.Default.withReportFailure(_ => ()).withTracing(Tracing.disabled)
}

case class ItemNotFound() extends Throwable
case class ItemOutOfStock() extends Throwable
case class NotAuthorized() extends Throwable
case class InsufficientFunds() extends Throwable
case class FraudDetected() extends Throwable

case class PurchaseDenied(msg: String, cause: Throwable)

case class Item(id: ItemId, name: String)
case class BoughtItem(item: Item)

type ItemId = String
type UserId = String
```
In the example below, we're trying to find an item and then buy it (providing a user ID).

It demonstrates some of the features of this library, such as:

* Handling errors partially (`mapErrorSome`)
* Handling errors exhaustively (`mapErrorAllInto`)
* Dying on certain errors (i.e. terminating the whole execution chain) 

```scala mdoc
def findItem(id: String): IO[OneOf2[ItemNotFound, ItemOutOfStock], Item] = 
  IO.fromEither {
    if (id == "itm1") Right(Item(id = "itm1", name = "Teddy Bear"))
    else Left(ItemNotFound().embedInto[OneOf2[ItemNotFound, ItemOutOfStock]])
  }


def buyItem(item: Item, userId: UserId): IO[OneOf3[FraudDetected, InsufficientFunds, NotAuthorized], BoughtItem] = {
  implicit val embedder: Embedder[OneOf3[FraudDetected, InsufficientFunds, NotAuthorized]] =
    Embedder.make
  IO.fromEither {
    if (userId == "frauduser") {
      Left(FraudDetected().embed)
    } else if (userId == "pooruser") {
      Left(InsufficientFunds().embed)
    } else {
      Right(BoughtItem(item))
    }
  }
}


def findAndBuy(
  itemId: ItemId,
  userId: UserId,
): IO[Nothing, String] = {
  implicit val embedder: Embedder[OneOf3[ItemNotFound, ItemOutOfStock, PurchaseDenied]] =
    Embedder.make
  (for {
    item <- findItem(itemId).embedError
    boughtItem <- buyItem(item, userId)

                   // Partial handling: converting some errors to another error
                   .mapErrorSome(
                     (e: InsufficientFunds) => PurchaseDenied("You don't have enough funds", e),
                     (e: NotAuthorized) =>
                       PurchaseDenied("You're not authorized to make purchases", e),
                   )
                   
                   // Terminate the whole computation if we encounter something fatal
                   .dieIf[FraudDetected]
                   .embedError
  } yield boughtItem)
    .flatMap(boughtItem => IO.succeed(s"Bought ${boughtItem.item.name}!"))

    // Exhaustive error handling, turning all errors into a user-friendly message
    .flatMapErrorAllInto[Nothing](
      (_: ItemNotFound)   => IO.succeed("item not found!"),
      (_: ItemOutOfStock) => IO.succeed("item out of stock"),
      (e: PurchaseDenied) => IO.succeed(s"Cannot purchase item because: ${e.msg}"),
    )
}

zioRuntime.unsafeRunSync(findAndBuy(itemId = "invalid_itm_id", userId = "user1"))
zioRuntime.unsafeRunSync(findAndBuy(itemId = "itm1", userId           = "pooruser"))
zioRuntime.unsafeRunSync(findAndBuy(itemId = "itm1", userId           = "frauduser")).toEither
zioRuntime.unsafeRunSync(findAndBuy(itemId = "itm1", userId           = "gooduser"))
```

