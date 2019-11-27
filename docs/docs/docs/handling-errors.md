---
layout: docs
title:  "Handling errors"
permalink: docs/handling-errors
---

Hotpotato gives you a lot of flexibility when it comes to handling errors.

Below is a table of the type of handling and what methods you can use

<table class="error-handling-table">
  <tr>
    <td style="border: none"></td>
    <td colspan="2">Converting Errors</td>
    <td colspan="2">Handling Errors</td>
  </tr>
  <tr>
    <td style="border: none"></td>
    <td>Some</td>
    <td>All</td>
    <td>Some</td>
    <td>All</td>
  </tr>
  <tr>
    <td>Pure</td>
    <td>mapSomeError</td>
    <td>mapAllError</td>
  </tr>
  <tr>
    <td>Effectful</td>
    <td>flatMapSomeError</td>
    <td>flatMapAllError</td>
    <td>handleSomeError</td>
    <td>flatMapErrorInto</td>
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
import hotpotato.ErrorTrans._
```

```scala mdoc
val result: Either[E1 :+: E2 :+: E3 :+: CNil, String] = returnsE1()

result.mapErrorInto(
  e1 => "e1",
  e2 => "e2",
  e3 => "e3"
)
```

There is `flatMapErrorInto`, if your error transformation returns :action GotoClass


## Handling all errors but into different types

Sometimes you want to transform each type of error into different types (e.g. 
Converting error that makes sense in one layer into errors for another layer/abstraction)


