package rebox

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._
import prop._
import org.scalacheck.Arbitrary._
import org.scalacheck._
import Gen._
import Arbitrary.arbitrary

import scala.collection.{immutable => im}
import scala.reflect.ClassTag

abstract class SetCheck[A: Arbitrary: ClassTag]
    extends PropSpec with Matchers with GeneratorDrivenPropertyChecks {

  def adhoc(as: Iterable[A]): rebox.Set[A] =
    as.foldLeft(rebox.Set.empty[A])(_ + _)

  property("from empty") {
    forAll { xs: im.Set[A] =>
      val r = adhoc(xs)
      r.foreach { x => xs(x) shouldBe true }
      xs.foreach { x => r(x) shouldBe true }
    }
  }

  property("from debox") {
    forAll { xs: im.Set[A] =>
      val d = debox.Set.fromIterable(xs)
      val r = rebox.Set(d)
      r.foreach { x => d(x) shouldBe true }
      d.foreach { x => r(x) shouldBe true }
    }
  }

  property("partial build") {
    forAll { (xs: im.Set[A], ys: im.Set[A]) =>
      val r1 = adhoc(xs | ys)
      val r2 = ys.foldLeft(rebox.Set(debox.Set.fromIterable(xs)))(_ + _)
      val r3 = rebox.Set(debox.Set.fromIterable(xs | ys))
      r1 shouldBe r2
      r2 shouldBe r3
    }
  }

  property("equals") {
    forAll { xs: im.Set[A] =>
      val r1 = adhoc(xs)
      val r2 = rebox.Set(debox.Set.fromIterable(xs))
      r1 shouldBe r2
      r2 shouldBe r1
    }
  }

  property("add (+) and remove (-)") {
    forAll { (xs: im.Set[A], m: List[(A, Boolean)]) =>
      val r1 = m.foldLeft(rebox.Set.fromIterable(xs)) { case (r, (k, b)) =>
        if (b) r + k else r - k
      }
      val r2 = rebox.Set.fromIterable(m.foldLeft(xs) { case (s, (k, b)) =>
        if (b) s + k else s - k
      })
      r1 shouldBe r2
      r1.size shouldBe r2.size
    }
  }

  property("toScala") {
    forAll { xs: im.Set[A] =>
      adhoc(xs).toScala shouldBe xs
      rebox.Set.fromIterable(xs).toScala shouldBe xs
    }
  }

  property("toDebox") {
    forAll { xs: im.Set[A] =>
      val d = debox.Set.fromIterable(xs)
      adhoc(xs).toDebox shouldBe d
      rebox.Set.fromIterable(xs).toDebox shouldBe d
    }
  }

  property("compact") {
    forAll { xs: im.Set[A] =>
      val r1 = adhoc(xs)
      val r2 = r1.compact
      r2 shouldBe r1
      r1.modSize shouldBe r1.size
      r2.modSize shouldBe 0
    }
  }
}

class BooleanSetCheck extends SetCheck[Boolean]
class IntSetCheck extends SetCheck[Int]
class DoubleSetCheck extends SetCheck[Double]
class StringSetCheck extends SetCheck[String]
