    import scala.language.implicitConversions

    object Tester {
      final case class Foo[A](value: A) {
        def flatMap[B](cat: Cat[A, B]): Foo[B] = {
          println("cat")
          cat.f(value)
        }

        def flatMap[B](monkey: Monkey[A, B]): Foo[B] = {
          println("monkey")
          monkey.f(value)
        }

        def map[B](f: A => B): Foo[B] = {
          Foo(f(value))
        }
      }

      case class Banana()
      class Monkey[A, B](val f: A => Foo[B])

      object Monkey {
        implicit def funcToMonkey[A, B](f: A => Foo[B])(implicit i: Banana): Monkey[A, B] = new Monkey(f)
      }
      case class Catnip()
      class Cat[A, B](val f: A => Foo[B])

      object Cat {
        implicit def funcToCat[A, B](f: A => Foo[B])(implicit i: Catnip): Cat[A, B] = new Cat(f)
      }

    }

    object Main {
      import Tester._
      def main(args: Array[String]) = {
//        Foo(1)
//          .flatMap(_ =>
//            Foo("asdf")
//              .map(_ => ())
//          )
      }
  }