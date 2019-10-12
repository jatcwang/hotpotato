package hotpotato.coproduct

import org.scalatest.{Matchers, WordSpec}
import shapeless.syntax.inject._
import hotpotato.Examples._
import shapeless._

class CombineSpec extends WordSpec with Matchers {
  "Combine" should {
    "Combine two coproducts with no overlaps elements" in {
      val i = implicitly[Combine.Aux[E1_E2, E3_E4, E1_E2_E3_E4]]
      i.right(e1.inject[E1_E2]) shouldBe Inl(e1)
      i.right(e2.inject[E1_E2]) shouldBe Inr(Inl(e2))
      i.left(e3.inject[E3_E4]) shouldBe Inr(Inr(Inl(e3)))
      i.left(e4.inject[E3_E4]) shouldBe Inr(Inr(Inr(Inl(e4))))
    }

    "Combine two coproduct with overlapping elements, keeping only uniques" in {
      val i = implicitly[Combine.Aux[E2_E3, E3_E4, E2_E3_E4]]
      i.right(e2.inject[E2_E3]) shouldBe Inl(e2)
      i.left(e3.inject[E3_E4]) shouldBe Inr(Inl(e3))
      i.left(e4.inject[E3_E4]) shouldBe Inr(Inr(Inl(e4)))
    }

    "Combine coproduct with itself should yield itself" in {
      val i = implicitly[Combine.Aux[E2_E3, E2_E3, E2_E3]]
      i.right(e2.inject[E2_E3]) shouldBe Inl(e2)
      i.left(e3.inject[E2_E3]) shouldBe Inr(Inl(e3))
    }
  }
}
