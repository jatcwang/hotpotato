---
layout: docs
title:  "Concepts"
permalink: docs/concepts
---

# Concepts

## Union types using Shapeless Coproducts
FIXME
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

