package rebox

import scala.{specialized => sp}
import scala.collection.{immutable => im, mutable => m}
import scala.reflect.ClassTag

import java.util.NoSuchElementException

object Map {
  def empty[K, V](implicit ctk: ClassTag[K], ctv: ClassTag[V]): Map[K, V] =
    new Map[K, V](0, im.Map.empty, debox.Map.empty)

  def apply[K, V](base: debox.Map[K, V]): Map[K, V] =
    new Map[K, V](base.size, im.Map.empty, base)
}

class Map[K, V] private[rebox] (override val size: Int, mods: im.Map[K, Option[V]], base: debox.Map[K, V]) extends Iterable[(K, V)] {

  def apply(k: K): V =
    mods.get(k) match {
      case Some(Some(v)) => v
      case Some(None) => throw new NoSuchElementException(k.toString)
      case None => base(k)
    }

  def updated(k: K, v: V): Map[K, V] =
    new Map(if (base.contains(k)) size else size + 1, mods.updated(k, Some(v)), base)

  def += (kv: (K, V)): Map[K, V] =
    updated(kv._1, kv._2)
  
  def -= (k: K): Map[K, V] =
    mods.get(k) match {
      case Some(Some(v)) =>
        new Map(size - 1, mods.updated(k, None), base)
      case Some(None) =>
        this
      case None =>
        if (base.contains(k)) new Map(size - 1, mods.updated(k, None), base) else this
    }
  
  def compact: Map[K, V] =
    Map(toDebox)
  
  def compactAt(percent: Double): Map[K, V] =
    if (mods.size > base.size * percent) compact else this
  
  override def foreach[U](f: Tuple2[K, V] => U): Unit = {
    mods.foreach { case (k, vo) => if (vo.isDefined) f(k, vo.get) }
    base.foreach { (k, v) => if (!mods.contains(k)) f((k, v)) }
  }

  def toScala: im.Map[K, V] = {
    val b = new m.MapBuilder[K, V, im.Map[K, V]](im.Map.empty)
    mods.foreach { case (k, vo) => if (vo.isDefined) b += ((k, vo.get)) }
    base.foreach { (k, v) => if (!mods.contains(k)) b += ((k, v)) }
    b.result
  }

  def toDebox: debox.Map[K, V] = {
    val child = base.copy
    mods.foreach { case (k, vo) =>
      if (vo.isDefined) child(k) = vo.get else child.remove(k)
    }
    child
  }

  def iterator: Iterator[(K, V)] =
    mods.iterator.flatMap { case (k, vo) =>
      if (vo.isDefined) Some((k, vo.get)) else None
    } ++ base.iterator.filter { case (k, _) =>
      !mods.contains(k)
    }
}
