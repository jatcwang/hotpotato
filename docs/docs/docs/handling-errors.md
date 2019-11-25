---
layout: docs
title:  "Handling errors"
permalink: docs/handling-errors
---

Hotpotato gives you a lot of flexibility when it comes to handling errors.

# Handling all errors

## handling all errors into the same type

Use `mapError1`, `mapError2`, etc (where the number corresponds to the number of errors you have)

```scala mdoc:invisible
import hotpotato.PureExamples._
import hotpotato.Examples._

def returnsE1() = b_E123_1
```

```scala mdoc:silent
import shapeless._
import hotpotato.ErrorTrans._
```

```scala mdoc
val result: Either[E1 :+: E2 :+: E3 :+: CNil, String] = returnsE1()

result.mapError3(
  e1 => "e1",
  e2 => "e2",
  e3 => "e3"
)
```

## Handling all errors but into different types

Sometimes you want to transform each type of error into different types (e.g. 
Converting error that makes sense in one layer into errors for another layer/abstraction)


