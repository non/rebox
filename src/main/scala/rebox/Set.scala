package rebox

import scala.{specialized => sp}
import scala.collection.{immutable => im, mutable => m}
import scala.reflect.ClassTag

import java.util.NoSuchElementException

object Set {
  def empty[A](implicit ct: ClassTag[A]): Set[A] =
    new Set[A](im.Set.empty, im.Set.empty, debox.Set.empty)

  def apply[A](base: debox.Set[A]): Set[A] =
    new Set[A](im.Set.empty, im.Set.empty, base)
}

class Set[A] private[rebox] (added: im.Set[A], removed: im.Set[A], base: debox.Set[A]) extends Iterable[A] {

  def apply(a: A): Boolean =
    added(a) || base(a) && !removed(a)

  def += (a: A): Set[A] =
    if (added(a)) this
    else if (base(a)) new Set(added, removed - a, base)
    else new Set(added + a, removed - a, base)

  def -= (a: A): Set[A] =
    if (base(a)) new Set(added - a, removed + a, base)
    else if (added(a)) new Set(added - a, removed, base)
    else this

  override def size: Int =
    added.size - removed.size + base.size

  def compact: Set[A] =
    Set(toDebox)

  def compactAt(percent: Double): Set[A] =
    if (added.size + removed.size > base.size * percent) compact else this

  override def foreach[U](f: A => U): Unit = {
    added.foreach(f)
    base.foreach { a => if (!added(a) && !removed(a)) f(a) }
  }

  def iterator: Iterator[A] =
    added.iterator ++ base.iterator.filter(a => !added(a) && !removed(a))

  def toScala: im.Set[A] = {
    val b = new m.SetBuilder[A, im.Set[A]](im.Set.empty)
    foreach { a => b += a }
    b.result
  }

  def toDebox: debox.Set[A] = {
    val child = base.copy
    removed.foreach { child -= _ }
    added.foreach { child += _ }
    child
  }
}
