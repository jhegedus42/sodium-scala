package sodium





import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class CellTester {
  import CellTester._

  @After def tearDown() {
    System.gc()
    Thread.sleep(100)
  }

  @Test
  def testHold() {
    val e = new StreamSink[Int]()
    val out = new ListBuffer[Int]()
    val l = e.hold(0).updates().listen(out.+=(_))
    List(2, 9).foreach(e.send(_))
    l.unlisten()
    assertEquals(List(2, 9), out)
  }

  @Test
  def testSnapshot() {
    val b = new CellSink(0)
    val trigger = new StreamSink[Long]()
    val out = new ListBuffer[String]()
    val l = trigger.snapshot[Int, String](b, (x, y) => x + " " + y).listen(out.+=(_))
    trigger.send(100L)
    b.send(2)
    trigger.send(200L)
    b.send(9)
    b.send(1)
    trigger.send(300L)
    l.unlisten()
    assertEquals(List("100 0", "200 2", "300 1"), out)
  }

  @Test
  def testValues() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = b.value().listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(9, 2, 7), out)
  }

  @Test
  def testConstantBehavior() {
    val b = new Cell(12)
    val out = new ListBuffer[Int]()
    val l = b.value().listen(out.+=(_))
    l.unlisten()
    assertEquals(List(12), out)
  }

  @Test
  def testValuesThenMap() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = b.value().map(x => x + 100).listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(109, 102, 107), out)
  }

  @Test
  def testValuesTwiceThenMap() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = doubleUp(b.value()).map(x => x + 100).listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(109, 109, 102, 102, 107, 107), out)
  }

  @Test
  def testValuesThenCoalesce() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = b.value().coalesce((fst, snd) => snd).listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(9, 2, 7), out)
  }

  @Test
  def testValuesTwiceThenCoalesce() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = doubleUp(b.value()).coalesce((fst, snd) => fst + snd).listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(18, 4, 14), out)
  }

  @Test
  def testValuesThenSnapshot() {
    val bi = new CellSink(9)
    val bc = new CellSink('a')
    val out = new ListBuffer[Character]()
    val l = bi.value().snapshot(bc).listen(out.+=(_))
    bc.send('b')
    bi.send(2)
    bc.send('c')
    bi.send(7)
    l.unlisten()
    assertEquals(List('a', 'b', 'c'), out)
  }

  @Test
  def testValuesTwiceThenSnapshot() {
    val bi = new CellSink(9)
    val bc = new CellSink('a')
    val out = new ListBuffer[Character]()
    val l = doubleUp(bi.value()).snapshot(bc).listen(out.+=(_))
    bc.send('b')
    bi.send(2)
    bc.send('c')
    bi.send(7)
    l.unlisten()
    assertEquals(List('a', 'a', 'b', 'b', 'c', 'c'), out)
  }

  @Test
  def testValuesThenMerge() {
    val bi = new CellSink(9)
    val bj = new CellSink(2)
    val out = new ListBuffer[Int]()
    val l = bi.value().merge(bj.value(), (x, y) => x + y).listen(out.+=(_))
    bi.send(1)
    bj.send(4)
    l.unlisten()
    assertEquals(List(11, 1, 4), out)
  }

  @Test
  def testValuesThenFilter() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = b.value().filter(a => true).listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(9, 2, 7), out)
  }

  @Test
  def testValuesTwiceThenFilter() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = doubleUp(b.value()).filter(a => true).listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(9, 9, 2, 2, 7, 7), out)
  }

  @Test
  def testValuesThenOnce() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = b.value().once().listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(9), out)
  }

  @Test
  def testValuesTwiceThenOnce() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val l = doubleUp(b.value()).once().listen(out.+=(_))
    List(2, 7).foreach(b.send(_))
    l.unlisten()
    assertEquals(List(9), out)
  }

  @Test
  def testValuesLateListen() {
    val b = new CellSink(9)
    val out = new ListBuffer[Int]()
    val value = b.value()
    b.send(8)
    val l = value.listen(out.+=(_))
    b.send(2)
    l.unlisten()
    assertEquals(List(8, 2), out)
  }

  @Test
  def testMapB() {
    val b = new CellSink(6)
    val out = new ListBuffer[String]()
    val l = b.map(x => x.toString()).value().listen(out.+=(_))
    b.send(8)
    l.unlisten()
    assertEquals(List("6", "8"), out)
  }

  def testMapBLateListen() {
    val b = new CellSink(6)
    val out = new ListBuffer[String]()
    val bm = b.map(x => x.toString())
    b.send(2)
    val l = bm.value().listen(out.+=(_))
    b.send(8)
    l.unlisten()
    assertEquals(List("2", "8"), out)
  }

  @Test
  def testTransaction() {
    var calledBack = false
    Transaction.run(trans => trans.prioritized(Node.NullNode, trans2 => calledBack = true))
    assertEquals(true, calledBack)
  }

  @Test
  def testApply() {
    val bf = new CellSink[Long => String](b => "1 " + b)
    val ba = new CellSink(5L)
    val out = new ListBuffer[String]()
    val l = Cell(bf, ba).value().listen(x => out.+=(x))
    bf.send(b => "12 " + b)
    ba.send(6L)
    l.unlisten()
    assertEquals(List("1 5", "12 5", "12 6"), out)
  }

  @Test
  def testLift() {
    val a = new CellSink(1)
    val b = new CellSink(5L)
    val out = new ListBuffer[String]()
    val l = Cell.lift[Int, Long, String]((x, y) => x + " " + y, a, b).value().listen(out.+=(_))
    a.send(12)
    b.send(6L)
    l.unlisten()
    assertEquals(List("1 5", "12 5", "12 6"), out)
  }

  @Test
  def testLiftGlitch() {
    val a = new CellSink(1)
    val a3 = a.map(x => x * 3)
    val a5 = a.map(x => x * 5)
    val out = new ListBuffer[String]()
    val l = Cell.lift[Int, Int, String]((x, y) => x + " " + y, a3, a5).value().listen(out.+=(_))
    a.send(2)
    l.unlisten()
    assertEquals(List("3 5", "6 10"), out)
  }

  @Test
  def testHoldIsDelayed() {
    val e = new StreamSink[Int]()
    val h = e.hold(0)
    val out = new ListBuffer[String]()
    val l = e.snapshot[Int, String](h, (a, b) => a + " " + b).listen(out.+=(_))
    List(2, 3).foreach(e.send(_))
    l.unlisten()
    assertEquals(List("2 0", "3 2"), out)
  }

  @Test
  def testSwitchB() {
    val esb = new StreamSink[SB]()
    // Split each field out of SB so we can update multiple behaviours in a
    // single transaction.
    val ba = esb.map(s => s.a).filterNotNull().hold('A')
    val bb = esb.map(s => s.b).filterNotNull().hold('a')
    val bsw = esb.map(s => s.sw).filterNotNull().hold(ba)
    val bo = Cell.switchC(bsw)
    val out = new ListBuffer[Character]()
    val l = bo.value().listen(out.+=(_))
    List(
      new SB('B', 'b', null),
      new SB('C', 'c', bb),
      new SB('D', 'd', null),
      new SB('E', 'e', ba),
      new SB('F', 'f', null),
      new SB(null, null, bb),
      new SB(null, null, ba),
      new SB('G', 'g', bb),
      new SB('H', 'h', ba),
      new SB('I', 'i', ba)).foreach(esb.send(_))
    l.unlisten()
    assertEquals(List('A', 'B', 'c', 'd', 'E', 'F', 'f', 'F', 'g', 'H', 'I'), out)
  }

  @Test
  def testSwitchE_Joco() {

    case class JSE(val a: Character, val b: Character, val sw: Stream[Character])

    val ese: StreamSink[JSE] = new StreamSink[JSE]()
    val ese2: StreamSink[Character] = new StreamSink[Character]()

    val ea: Stream[Character] = ese.map(s => s.a).filterNotNull()
    val eb: Stream[Character] = ese.map(s => s.b).filterNotNull()

    val bsw: Cell[Stream[Character]] = ese.map(s => s.sw).filterNotNull().hold(ea)
    val out = new ListBuffer[Char]()

    val eo: Stream[Character] = Cell.switchS(bsw)
    val l = eo.listen(out.+=(_))

    List(
      new JSE('A', 'a', null),
      new JSE('B', 'b', null),
      new JSE('C', 'c', eb),
      new JSE('D', 'd', null),
      new JSE('E', 'e', ea),
      new JSE('F', 'f', null),
      new JSE('G', 'g', eb),
      new JSE('H', 'h', ea),
      new JSE('I', 'i', ese2),
      new JSE('J', 'j', null)
    ).foreach(ese.send(_))

    ese2.send('j')
    l.unlisten()
//    assertEquals(List('A', 'B', 'C', 'd', 'e', 'F', 'G', 'h', 'I','J'), out)
    assertEquals(List('A', 'B', 'C', 'd', 'e', 'F', 'G', 'h', 'I','j'), out)
  }

  @Test
  def testSwitchE() {
    val ese: StreamSink[SE] = new StreamSink[SE]()
    val ea: Stream[Character] = ese.map(s => s.a).filterNotNull()
    val eb: Stream[Character] = ese.map(s => s.b).filterNotNull()
    val bsw: Cell[Stream[Character]] = ese.map(s => s.sw).filterNotNull().hold(ea)
    val out = new ListBuffer[Char]()
    val eo: Stream[Character] = Cell.switchS(bsw)
    val l = eo.listen(out.+=(_))
    List(
      new SE('A', 'a', null),
      new SE('B', 'b', null),
      new SE('C', 'c', eb),
      new SE('D', 'd', null),
      new SE('E', 'e', ea),
      new SE('F', 'f', null),
      new SE('G', 'g', eb),
      new SE('H', 'h', ea),
      new SE('I', 'i', ea)).foreach(ese.send(_))
    l.unlisten()
    assertEquals(List('A', 'B', 'C', 'd', 'e', 'F', 'G', 'h', 'I'), out)
  }

  @Test
  def testLoopBehavior() {
    val ea = new StreamSink[Int]()
    val sum_out = Transaction(_ => {
      val sum = new CellLoop[Int]()
      val sum_out_ = ea.snapshot[Int, Int](sum, (x, y) => x + y).hold(0)
      sum.loop(sum_out_)
      sum_out_
    })
    val out = new ListBuffer[Int]()
    val l = sum_out.value().listen(out.+=(_))
    List(2, 3, 1).foreach(ea.send(_))
    l.unlisten()
    assertEquals(List(0, 2, 5, 6), out)
    assertEquals(6, sum_out.sample())
  }

  @Test
  def testCollect() {
    val ea = new StreamSink[Int]()
    val out = new ListBuffer[Int]()
    val sum = ea.hold(100).collect[Int, Int](0, (a, s) => (a + s, a + s))
    val l = sum.value().listen(out.+=(_))
    List(5, 7, 1, 2, 3).foreach(ea.send(_))
    l.unlisten()
    assertEquals(List(100, 105, 112, 113, 115, 118), out)
  }

  @Test
  def testAccum() {
    val ea = new StreamSink[Int]()
    val out = new ListBuffer[Int]()
    val sum = ea.accum[Int](100, (a, s) => a + s)
    val l = sum.value().listen(out.+=(_))
    List(5, 7, 1, 2, 3).foreach(ea.send(_))
    l.unlisten()
    assertEquals(List(100, 105, 112, 113, 115, 118), out)
  }

  @Test
  def testLoopValueSnapshot() {
    val out = new ListBuffer[String]()
    val eSnap = Transaction(_ => {
      val a = new Cell("lettuce")
      val b = new CellLoop[String]()
      val eSnap_ = a.value().snapshot[String, String](b, (aa, bb) => aa + " " + bb)
      b.loop(new Cell[String]("cheese"))
      eSnap_
    })
    val l = eSnap.listen(out.+=(_))
    l.unlisten()
    assertEquals(List("lettuce cheese"), out)
  }

  @Test
  def testLoopValueHold() {
    val out = new ListBuffer[String]()
    val value = Transaction(_ => {
      val a = new CellLoop[String]()
      val value_ = a.value().hold("onion")
      a.loop(new Cell[String]("cheese"))
      value_
    })
    val eTick = new StreamSink[Int]()
    val l = eTick.snapshot(value).listen(out.+=(_))
    eTick.send(0)
    l.unlisten()
    assertEquals(List("cheese"), out)
  }

  @Test
  def testLiftLoop() {
    val out = new ListBuffer[String]()
    val b = new CellSink("kettle")
    val c = Transaction(_ => {
      val a = new CellLoop[String]()
      val c_ = Cell.lift[String, String, String]((aa, bb) => aa + " " + bb, a, b)
      a.loop(new Cell[String]("tea"))
      c_
    })
    val l = c.value().listen(out.+=(_))
    b.send("caddy")
    l.unlisten()
    assertEquals(List("tea kettle", "tea caddy"), out)
  }
}

object CellTester {

  /**
   * This is used for tests where value() produces a single initial value on listen,
   * and then we double that up by causing that single initial event to be repeated.
   * This needs testing separately, because the code must be done carefully to achieve
   * this.
   */
  private def doubleUp(ev: Stream[Int]): Stream[Int] = ev.merge(ev)
}

case class SB(val a: Character, val b: Character, val sw: Cell[Character])

case class SE(val a: Character, val b: Character, val sw: Stream[Character])

