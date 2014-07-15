## Rebox

### Overview

Rebox is a companion package to [Debox]().

Rebox is designed to provide an immutable veneer around Debox
instances, including immutable updating with structural sharing.  The
ideal use case is a situation where a large amount of unboxed data is
needed, but where an immutable collection (which can be updated) is
required.

The name refers to the fact that Rebox's immutability and
structural-sharing come at the cost of additional boxing.

### Examples

```scala
// construct a very large debox.Set instance, and wrap it in
// rebox.Set for safety. this will save space, while providing
// a generic and immutable facade (but will cause boxing).
def load(...): rebox.Set[Int] = {
  // lots of unboxed values hashed into an int[].
  val largeSet: debox.Set[Int] = ...

  // create a threadsafe, immutable veneer
  rebox.Set(largeSet)
}

// create a rebox.Set instance to play with.
val values1 = load(...)

// does not copy underlying debox.Set instance.
// adds 999 to an additional internal immutable.Set[Int].
// values1 is still totally viable and unchanged.
val values2 = values1 += 999

// still shares underlying int[] with values1 and values2
// internal immutable.Set[Int] contains all "new" values.
val values3 = (1001 to 14255).foldLeft(values2) { (set, n) => set += n }

// updates an additional Set[Int] to track deletions.
// structurally sharing additions with values3 and
// the int[] with all previous rebox.Set instances.
val values4 = values3 - 1111

// allocates new int[] with all "current" values.
// if values1 through values4 are freed, the original
// underlying array will be freed too.
val values5 = values4.compact
```

### Getting Rebox

Rebox is published to [bintray](https://bintray.com/) using the
[bintray-sbt](https://github.com/softprops/bintray-sbt) plugin.

Zillion supports Scala 2.10 and 2.11. If you use SBT, you can
include Zillion via the following `build.sbt` snippets:

```
resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

libraryDependencies += "org.spire-math" %% "rebox" % "0.0.1"
```

### Details

Rebox's design is based around the idea of shared, read-only Debox
instances along with an immutable "changelog". This means that
additions and deletions will not have to copy a large array, but will
use the immutable map and set instances that Scala is known for.  It
also means that Rebox's immutable instances are threadsafe (and use
structural sharing) while also conserving underlying storage space.

One critical detail is that the underlying Debox instances must be
considered read-only. This invariant is not enforced by the code;
users who wish to code defensively can use the `.copy` method to
create a fresh Debox instance.

```scala
val largeSet: debox.Set[Double] = ...

// as long as no one else modifies largeSet this is fine
val trusting: rebox.Set[Double] = rebox.Set(largeSet)

// this is a safer option, but duplicates largeSet's contents
val safe: rebox.Set[Double] = rebox.Set(largeSet.copy)
```

After a Rebox instance accumulates a lot of changes, it will start
using up more space. Users can manually compact Rebox instances in two
ways:

```scala
val items: rebox.Map[K, V] = ...

// unconditionally compact the items, ensuring a maximally-efficient
// unboxed representation.
val a = items.compact

// only compact the items if the given storage ratio is exceed. the
// percentage parameter (0.5 in this case) means that if the number
// of changes reaches 50% of the underlying map's size, compacting
// will occur.
b = items.compactAt(0.5)
```

### Known Issues and Future Work

Rebox is not specialized. While immutable structures will always do
more boxing than a "raw" array-based approach it's possible we can do
much better here.

Rebox does not do any automatic compacting. It might be the case that
there is an optimal percentage at which it should automatically
compact.

Rebox instances only implement the `Iterable` interface, and don't
support many collections methods directly. There are definitely
additional methods we should be overriding, and it's also possible we
should be trying to use `SetLike` and `MapLike` (although getting that
working correctly will be a thankless task). This means that equality
tests with Scala's set and map classes will always return `false`.

Rebox could use more benchmarks (both performance and memory).

### Copyright and License

All code is available to you under the MIT license, available at
http://opensource.org/licenses/mit-license.php and also in the
[COPYING](COPYING) file.

Copyright Erik Osheim, 2014.
