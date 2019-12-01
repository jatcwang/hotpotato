---
layout: home
title:  "Home"
section: "home"
position: 1
---

A type-safe and flexible error handling library for Scala, based on Shapeless Coproducts.

[![Release](https://img.shields.io/nexus/r/com.github.jatcwang/hotpotato-core_2.13?server=https%3A%2F%2Foss.sonatype.org)](https://oss.sonatype.org/content/repositories/releases/com/github/jatcwang/hotpotato-core_2.13/)
[![Join the chat at https://gitter.im/jatcwang/hotpotato](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jatcwang/hotpotato)

# Installation

```
libraryDependencies += "com.github.jatcwang" %% "hotpotato-core" % LATEST_VERSION
```

# Use it!
```scala mdoc:invisible
case class SoldOut()
case class DoesntExist()
case class TooPoor(excuse: String)
case class StruckByLightning() extends Throwable

case class Item(id: Int, name: String)
case class Box(item: Item)
```
```scala mdoc:silent
import zio._
import hotpotato.implicits._
import hotpotato.Embedder
import shapeless._ // Coproduct type and functions to construct them
import shapeless.syntax.inject._

def findItem(): IO[SoldOut :+: DoesntExist :+: CNil, Item] = IO.succeed {
  Item(id = 123, name = "Teddy Bear")
}
def buyAndBoxItem(item: Item): IO[TooPoor :+: CNil, Box] = IO.fail(TooPoor("studied_hard").inject)

def openBox(box: Box): IO[StruckByLightning :+: CNil, Item] = IO.succeed(box.item)
```

```scala mdoc
implicit val embedder: Embedder[SoldOut :+: DoesntExist :+: TooPoor :+: CNil] = Embedder.make
for {
  item <- findItem()
  box <- buyAndBoxItem(item).flatMapErrorAllInto(
           tooPoor => if (tooPoor.excuse == "studied_hard")
                        IO.succeed(Box(item)) // ask mum to buy it if we're too poor
                      else IO.fail(tooPoor.inject[TooPoor :+: CNil])
         ).embedError
  openResult <- openBox(box).dieIf[StruckByLightning].embedError
} yield openResult
```

