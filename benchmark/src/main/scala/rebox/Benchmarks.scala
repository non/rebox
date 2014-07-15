package rebox

import scala.collection.{immutable => im}
import scala.reflect.ClassTag
import scala.util.Random

import spire.syntax.cfor._

import com.google.caliper.{Benchmark, Param, SimpleBenchmark, Runner}

abstract class MyRunner(cls:java.lang.Class[_ <: Benchmark]) {
  def main(args:Array[String]): Unit = Runner.main(cls, args:_*)
}

trait MyBenchmark extends SimpleBenchmark {
  def init[A:ClassTag](size:Int)(init: => A) = {
    val data = Array.ofDim[A](size)
    for (i <- 0 until size) data(i) = init
    data
  }

  def run(reps:Int)(f: => Unit) = for(i <- 0 until reps)(f)
}

object SetBenchmarks extends MyRunner(classOf[SetBenchmarks])

class SetBenchmarks extends MyBenchmark {

  @Param(Array("1000", "10000", "100000", "1000000"))
  var size: Int = 0

  var d: debox.Set[Int] = _
  var s: im.Set[Int] = _

  var r100: rebox.Set[Int] = _
  var r99: rebox.Set[Int] = _
  var r90: rebox.Set[Int] = _
  var r80: rebox.Set[Int] = _

  var keys: Array[Int] = _

  def reboxPercent(arr: Array[Int], p: Double): rebox.Set[Int] = {
    val limit = (arr.size * p).toInt
    rebox.Set.fromIterable(s.take(limit)) ++ arr.drop(limit)
  }

  override protected def setUp() {
    val arr = init(size)(Random.nextInt)
    s = im.Set(arr: _*)
    d = debox.Set.fromIterable(s)

    r100 = rebox.Set.fromIterable(s)
    r99 = reboxPercent(arr, 0.99)
    r90 = reboxPercent(arr, 0.90)
    r80 = reboxPercent(arr, 0.80)

    keys = init(size)(Random.nextInt)
    for(i <- 0 until size) if (Random.nextInt(2) == 1) keys(i) = arr(i)
  }

  def timeScala(reps: Int) = run(reps) {
    var total = 0
    cfor(0)(_ < size, _ + 1) { i =>
      if (s(i)) total += 1
    }
    total
  }

  def timeDebox(reps: Int) = run(reps) {
    var total = 0
    cfor(0)(_ < size, _ + 1) { i =>
      if (d(i)) total += 1
    }
    total
  }

  def timeRebox100(reps: Int) = run(reps) {
    var total = 0
    cfor(0)(_ < size, _ + 1) { i =>
      if (r100(i)) total += 1
    }
    total
  }

  def timeRebox99(reps: Int) = run(reps) {
    var total = 0
    cfor(0)(_ < size, _ + 1) { i =>
      if (r99(i)) total += 1
    }
    total
  }

  def timeRebox90(reps: Int) = run(reps) {
    var total = 0
    cfor(0)(_ < size, _ + 1) { i =>
      if (r90(i)) total += 1
    }
    total
  }

  def timeRebox80(reps: Int) = run(reps) {
    var total = 0
    cfor(0)(_ < size, _ + 1) { i =>
      if (r80(i)) total += 1
    }
    total
  }
}
