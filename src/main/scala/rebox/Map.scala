package rebox

import scala.{specialized => sp}
import scala.collection.{immutable => im, mutable => m}
import scala.reflect.ClassTag

import java.util.NoSuchElementException

object Map {
  def empty[K: ClassTag, V: ClassTag]: Map[K, V] =
    new Map[K, V](0, im.Map.empty, debox.Map.empty)

  def apply[K, V](base: debox.Map[K, V]): Map[K, V] =
    new Map[K, V](base.size, im.Map.empty, base)

  def fromIterable[K: ClassTag, V: ClassTag](kvs: Iterable[(K, V)]): Map[K, V] =
    apply(debox.Map.fromIterable(kvs))
}

class Map[K, V] private[rebox] (override val size: Int, mods: im.Map[K, Option[V]], private[rebox] val base: debox.Map[K, V]) extends Iterable[(K, V)] {

  override def hashCode: Int =
    foldLeft(0xba55d00d) { case (n, (k, v)) => n ^ ((k.## * 9913) + (v.## * 551) + 23953) }

  override def equals(that: Any): Boolean =
    that match {
      case r: rebox.Map[_, _] =>
        if (r.size != size || r.base.cta != base.cta || r.base.ctb != base.ctb) return false
        val rr = r.asInstanceOf[Map[K, V]]
        rr.forall { case (k, v) =>
          val o = get(k)
          o.isDefined && o.get == v
        }
      case _ =>
        false
    }

  def modSize: Int = mods.size

  def apply(k: K): V =
    mods.get(k) match {
      case Some(Some(v)) => v
      case Some(None) => throw new NoSuchElementException(k.toString)
      case None => base(k)
    }

  def get(k: K): Option[V] =
    mods.get(k) match {
      case Some(o) => o
      case None => base.get(k)
    }

  def updated(k: K, v: V): Map[K, V] = {
    val n = mods.get(k) match {
      case Some(o) => if (o.isDefined) size else size + 1
      case None => if (base.contains(k)) size else size + 1
    }
    new Map(n, mods.updated(k, Some(v)), base)
  }

  def remove(k: K): Map[K, V] =
    mods.get(k) match {
      case Some(Some(_)) =>
        new Map(size - 1, mods.updated(k, None), base)
      case Some(None) =>
        this
      case None =>
        if (base.contains(k)) new Map(size - 1, mods.updated(k, None), base) else this
    }

  def + (kv: (K, V)): Map[K, V] =
    updated(kv._1, kv._2)
  
  def - (k: K): Map[K, V] =
    remove(k)
  
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
