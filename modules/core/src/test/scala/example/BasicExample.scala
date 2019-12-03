package example

object BasicExample {
  import hotpotato._
  import zio._
  import zio.internal.{Platform, PlatformLive, Tracing}

  val zioRuntime: DefaultRuntime = new DefaultRuntime {
    override val platform: Platform =
      PlatformLive.Default.withReportFailure(_ => ()).withTracing(Tracing.disabled)
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

  def findItemImpl(
    id: String,
  ): IO[OneOf2[ItemNotFound, ItemOutOfStock], Item] =
    IO.fromEither {
      if (id == "itm1") Right(Item(id = "itm1", name = "Teddy Bear"))
      else Left(ItemNotFound().embedInto[OneOf2[ItemNotFound, ItemOutOfStock]])
    }

  def buyItemImpl(
    item: Item,
    userId: UserId,
  ): IO[OneOf3[FraudDetected, InsufficientFunds, NotAuthorized], BoughtItem] = {
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

  def findItem(
    id: String,
  ): IO[OneOf2[ItemNotFound, ItemOutOfStock], Item] = findItemImpl(id)

  def buyItem(
    item: Item,
    userId: UserId,
  ): IO[OneOf3[FraudDetected, InsufficientFunds, NotAuthorized], BoughtItem] =
    buyItemImpl(item, userId)

  def findAndBuy(
    itemId: ItemId,
    userId: UserId,
  ): IO[Nothing, String] = {
    implicit val embedder: Embedder[OneOf3[ItemNotFound, ItemOutOfStock, PurchaseDenied]] =
      Embedder.make
    (for {
      item <- findItem(itemId).embedError
      boughtItem <- buyItem(item, userId)
                   // Partial handling, converting to a more general error
                     .mapErrorSome(
                       (e: InsufficientFunds) => PurchaseDenied("You don't have enough funds", e),
                       (e: NotAuthorized) =>
                         PurchaseDenied("You're not authorized to make purchases", e),
                     )
                     .dieIf[FraudDetected]
                     .embedError
    } yield boughtItem)
      .flatMap(boughtItem => IO.succeed(s"Bought ${boughtItem.item.name}!"))
      // Exhaustive error handling, in this case we turn it into a user-friendly message
      .flatMapErrorAllInto[Nothing](
        (e: ItemNotFound)   => IO.succeed("item not found!"),
        (e: ItemOutOfStock) => IO.succeed("item out of stock"),
        (e: PurchaseDenied) => IO.succeed(s"Cannot purchase item because: ${e.msg}"),
      )
  }

  zioRuntime.unsafeRunSync(findAndBuy(itemId = "invalid_itm_id", userId = "user1"))
  zioRuntime.unsafeRunSync(findAndBuy(itemId = "itm1", userId           = "pooruser"))
  zioRuntime.unsafeRunSync(findAndBuy(itemId = "itm1", userId = "frauduser")).toEither
  zioRuntime.unsafeRunSync(findAndBuy(itemId = "itm1", userId = "gooduser"))
}
