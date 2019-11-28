---
layout: docs
title:  "FAQ"
permalink: docs/faq
---

# Frequently Asked Questions

## Help! The IDE can't figure out what the error type should be!

We provide a helper method `errorIs` which you can use to annotate
the expected error type, which will help your IDE infer the error type of the
following calls.

Note that the `Coproduct` instance of a `sealed trait` is alphabetically ordered!
```scala mdoc:invisible
import hotpotato.ErrorTrans._
import shapeless._
```

```scala mdoc:silent
sealed trait Sealed
case class C() extends Sealed
case class B() extends Sealed
case class A() extends Sealed
```
```scala mdoc
val either: Either[Sealed, Unit] = Left(C())

either.errorAsCoproduct.errorIs[A :+: B :+: C :+: CNil]

// Or for errorAsCoproduct specifically..
either.errorAsCoproduct[A :+: B :+: C :+: CNil]
```

