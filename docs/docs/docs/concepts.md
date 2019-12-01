---
layout: docs
title:  "Concepts"
permalink: docs/concepts
---

# Concepts

## Union types using Shapeless Coproducts

You can think Coproducts as `Either` type that is specialized for representing multiple possibilities (instead of just 2)

For example, if we want to represent "Int or Double or Boolean", we can use the Either type

`Either[Int, Either[Double, Boolean]]`, or in infix form `Int Either (Double Either Boolean)`

You can use Coproducts to represent the same idea:

`Int :+: Double :+: Boolean :+: CNil`

where `CNil` is used to denote the end of a Coproduct. You can think of Coproducts as a list of types, and at runtime
the actual value will be one of the types in the list. (Note that like nesting `Either`s, you can have duplicate types
in your nesting `Either`, however when using `hotpotato` this isn't something you need to worry about)

To make it easier to read and type, `hotpotato` provides the type `OneOf{1,2,3,4...}` which are type synonyms for
the above Coproduct syntax. For example, you can use `OneOf3` to represent the exact same coproduct above:

`OneOf3[Int, Double, Boolean]`

## Error handling strategies

When faced with a few possible errors (i.e. a union of multiple errors),
you may want to

* Exhaustively handle all error cases
* Only handle some cases and leave others for the caller of your function to handle

For example:

* When deciding what the HTTP response should be, you probably want to handle 
  all error cases exhaustively and map them to specific HTTP status codes
* When writing business logic, you may want to expose `PermissionDenied` to your caller,
  while converting errors from the implementation like `NetworkError` into `GenericError`
  
### Handling errors

Given an error, there are a few things you might want to do with it:

* Turn it into another error (e.g. by wrapping it)
* Try to recover from it by running some side effect
* Perform some side effect and consider it "handled". (e.g. logging an error message)

