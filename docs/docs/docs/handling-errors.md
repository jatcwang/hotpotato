---
layout: docs
title:  "Handling errors"
permalink: docs/handling-errors
---

Hotpotato gives you a lot of flexibility when it comes to handling errors.

Below is a table of the type of handling and what methods you can use

<style>
    .error-handling-table td{
      text-align: center;
    }
    
    .error-handling-table .left {
      text-align: left;
    }
    
    .bold {
      font-weight: bold;
    }
</style>

<table class="error-handling-table">
  <tr>
    <td style="border: none"></td>
    <td colspan="2" class="bold">Converting Errors</td>
  </tr>
  <tr>
    <td style="border: none"></td>
    <td class="bold">Some</td>
    <td class="bold">All</td>
  </tr>
  <tr>
    <td class="left bold">Pure</td>
    <td>mapErrorSome</td>
    <td>mapErrorAll / mapErrorAllInto</td>
  </tr>
  <tr>
    <td class="left bold">Effectful</td>
    <td>flatMapErrorSome</td>
    <td>flatMapErrorAll / flatMapErrorAllInto</td>
  </tr>
</table>

# Handling all errors

Required import:
```scala mdoc:silent
import hotpotato._
```

```scala mdoc:invisible
import hotpotato.PureExamples._
import hotpotato.Examples._

def returnsE1() = b_E123_1
```

Example value we will be operating on:
```scala mdoc
val result: Either[OneOf3[E1, E2, E3], String] = returnsE1()
```

## mapErrorAll: transforming all errors into different types
```scala mdoc
val x: Either[OneOf2[X2, X1], String] = result.mapErrorAll(
  (e1: E1) => X1(e1),
  (e2: E2) => X2(e2),
  (e3: E3) => X1(e3)
)
```

Note that duplicate types are deduplicated automatically.

## mapErrorAllInto: transforming all errors into the same type

For example, you might want to convert all errors into an error message for the user.

```scala mdoc
result.mapErrorAllInto(
  e1 => s"Error is $e1",
  e2 => s"Error is $e2",
  e3 => s"Error is $e3",
)
```

