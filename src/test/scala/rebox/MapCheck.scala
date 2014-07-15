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

abstract class MapCheck[K: Arbitrary: ClassTag, V: Arbitrary: ClassTag]
    extends PropSpec with Matchers with GeneratorDrivenPropertyChecks {

  def adhoc(kvs: Iterable[(K, V)]): rebox.Map[K, V] =
    kvs.foldLeft(rebox.Map.empty[K, V]) { case (m, (k, v)) => m.updated(k, v) }

  property("from empty") {
    forAll { m: im.Map[K, V] =>
      val r = adhoc(m)
      r.size shouldBe m.size
      r.foreach { case (k, v) => m.get(k) shouldBe Some(v) }
      m.foreach { case (k, v) => r.get(k) shouldBe Some(v) }
    }
  }

  property("from debox") {
    forAll { m: im.Map[K, V] =>
      val d = debox.Map.fromIterable(m)
      val r = rebox.Map(d)
      r.size shouldBe d.size
      r.foreach { case (k, v) => d.get(k) shouldBe Some(v) }
      d.foreach { case (k, v) => r.get(k) shouldBe Some(v) }
    }
  }

  property("partial build") {
    forAll { (m1: im.Map[K, V], m2: im.Map[K, V]) =>
      val r1 = adhoc(m1 ++ m2)
      val r2 = m2.foldLeft(rebox.Map(debox.Map.fromIterable(m1))) { case (r, (k, v)) => r.updated(k, v) }
      val r3 = rebox.Map(debox.Map.fromIterable(m1 ++ m2))
      r1 shouldBe r2
      r2 shouldBe r3
      r1.## shouldBe r2.##
      r2.## shouldBe r3.##
    }
  }

  property("equals") {
    forAll { xs: im.Map[K, V] =>
      val r1 = adhoc(xs)
      val r2 = rebox.Map(debox.Map.fromIterable(xs))
      r1 shouldBe r2
      r2 shouldBe r1
      r1.## shouldBe r2.##
    }
  }

  property("remove (-)") {
    forAll { m: im.Map[K, V] =>
      var test = rebox.Map.fromIterable(m)
      var control = m
      m.foreach { case (k, v) =>
        test = test - k
        control = control - k

        test.toScala shouldBe control
        test.size shouldBe control.size
        test shouldBe rebox.Map.fromIterable(control)
      }
    }
  }

  property("add (+) and remove (-)") {
    forAll { (xs: im.Map[K, V], m: List[(K, Option[V])]) =>
      val r1 = m.foldLeft(rebox.Map.fromIterable(xs)) {
        case (r, (k, Some(v))) => r.updated(k, v)
        case (r, (k, None)) => r - k
      }
      val r2 = rebox.Map.fromIterable(m.foldLeft(xs) {
        case (s, (k, Some(v))) => s.updated(k, v)
        case (s, (k, None)) => s - k
      })
      r1 shouldBe r2
      r1.size shouldBe r2.size
    }
  }

  property("toScala") {
    forAll { m: im.Map[K, V] =>
      adhoc(m).toScala shouldBe m
      rebox.Map.fromIterable(m).toScala shouldBe m
    }
  }

  property("toDebox") {
    forAll { m: im.Map[K, V] =>
      val d = debox.Map.fromIterable(m)
      adhoc(m).toDebox shouldBe d
      rebox.Map.fromIterable(m).toDebox shouldBe d
    }
  }

  property("compact") {
    forAll { m: im.Map[K, V] =>
      val r1 = adhoc(m)
      val r2 = r1.compact
      r2 shouldBe r1
      r1.modSize shouldBe r1.size
      r2.modSize shouldBe 0
    }
  }
}

class StringBooleanMapCheck extends MapCheck[String, Boolean]
class IntDoubleMapCheck extends MapCheck[Int, Double]
class StringDoubleMapCheck extends MapCheck[String, Double]
class ByteStringMapCheck extends MapCheck[Byte, String]
