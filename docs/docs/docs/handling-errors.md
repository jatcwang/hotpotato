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

## handling all errors into the same type

Use `mapErrorInto`

```scala mdoc:invisible
import hotpotato.PureExamples._
import hotpotato.Examples._

def returnsE1() = b_E123_1
```

```scala mdoc:silent
import shapeless._
import hotpotato.implicits._
import hotpotato._
```

```scala mdoc
val result: Either[OneOf3[E1, E2, E3], String] = returnsE1()

result.mapErrorAllInto(
  e1 => "e1",
  e2 => "e2",
  e3 => "e3"
)
```

There is `flatMapErrorInto`, if your error transformation returns :action GotoClass


## Handling all errors but into different types

Sometimes you want to transform each type of error into different types (e.g. 
Converting error that makes sense in one layer into errors for another layer/abstraction)


