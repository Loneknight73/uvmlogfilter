/*
    uvmlogfilter - A Java program to filter UVM logs
    Copyright (C) 2019-present  Loneknight73

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

 */

package uvmlog

import scala.collection.mutable.ListBuffer
import scala.io.Source

case class LogRecord(severity: String,
                     file: String,
                     linen: Int,
                     time: Int,
                     hier: String,
                     id: String,
                     var message: ListBuffer[String])

object LogRecord {
  def printRecord(l: LogRecord) = {
    for (line <- l.message) {
      println(line)
    }
  }
}

class UvmLogFilter(val filename: String) {

  val recordList: scala.collection.mutable.ListBuffer[LogRecord] = ListBuffer()
  var currRecord: Option[LogRecord] = None

  for (line <- Source.fromFile(filename).getLines) {
    //println(line)
    val x = getUvmMessage(line)
    x match {
      case Some(lr) => {
        //println(s"Found match: ${line}")
        recordList += lr
        lr.message += line
        currRecord = Some(lr)
      }
      case None => {
        currRecord match {
          case Some(r) => r.message += line
          case None => ()
        }
      }
    }
  }

  def getUvmMessage(line: String): Option[LogRecord] = {
    val severityPatt = "(UVM_INFO|UVM_WARNING|UVM_ERROR|UVM_FATAL)"
    val idPatt = "\\[(.*)\\]"
    val filePatt = "([\\S].*)"
    val linenPatt = "\\(([\\d]+)\\)"
    val timePatt = "@\\s+([\\d]+)" // TODO: update to take time units into account
    val hierPatt = "([\\S]+)"
    val uvmregex = s"${severityPatt}\\s+${filePatt}${linenPatt}\\s+${timePatt}:\\s+${hierPatt}.*${idPatt}".r.unanchored

    line match {
      case uvmregex(severity, file, linen, time, hier, id) => {
        Some(LogRecord(severity, file, linen.toInt, time.toInt, hier, id, ListBuffer()))
      }
      case _ => None
    }
  }

  def getFiltered(lrf: LogRecordFilter): ListBuffer[LogRecord] = {
    recordList.filter(lrf.f)
  }
}

trait LogRecordFilter {
  def f: LogRecord => Boolean

  def ||(f: LogRecordFilter) = LogRecordFilter((l: LogRecord) => (this.f(l) || f.f(l)))

  def &&(f: LogRecordFilter) = LogRecordFilter((l: LogRecord) => (this.f(l) && f.f(l)))

  def negate() = LogRecordFilter((l: LogRecord) => !(this.f(l)))
}

case class SeverityLogRecordFilter(s: String) extends LogRecordFilter {

  def f: LogRecord => Boolean = { l =>
    if (l.severity == s) true else false
  }

  override def toString: String = {
    "Severity is " + s"\'${s}\'"
  }
}

case class IdLogRecordFilter(s: String, matchtype: String) extends LogRecordFilter {
  def f: LogRecord => Boolean = { l =>
    //TODO: is
    if (l.id.contains(s)) true else false
  }

  override def toString: String = {
    "id " + s"${matchtype} " + s"\'${s}\'"
  }
}

case class TimeLogRecordFilter(min: BigInt, max: BigInt) extends LogRecordFilter {
  def f: LogRecord => Boolean = { l =>
    if (l.time >= min && l.time <= max) true else false
  }

  override def toString: String = {
    s"time inside [${min}:${max}]"
  }
}

case class HierLogRecordFilter(h: String) extends LogRecordFilter {
  def f: LogRecord => Boolean = { l =>
    if (l.hier startsWith (h)) true else false
  }

  override def toString: String = {
    s"Under hierarchy ${h}"
  }
}

case class CompNameLogRecordFilter(c: String) extends LogRecordFilter {
  private val patt = "(\\w+\\.)*(\\w+)+".r

  def f: LogRecord => Boolean = { l: LogRecord => {
    l.hier match {
      case patt(_, compName) => {
        if (compName == c) true else false
      }
      case _ => false
    }
  }
  }

  override def toString: String = {
    s"Component is ${c}"
  }
}

case class TextContainsLogRecordFilter(s: String) extends LogRecordFilter {

  def f: LogRecord => Boolean = { l: LogRecord =>
    var b = false
    for (line <- l.message) {
      if (line contains (s)) {
        b |= true
      }
    }
    b
  }

  override def toString: String = {
    s"Text contains ${s}"
  }
}

case class TrueLogRecordFilter() extends LogRecordFilter {
  def f = l => true
}

case class FalseLogRecordFilter() extends LogRecordFilter {
  def f = l => false
}

object LogRecordFilter {
  def apply(x: LogRecord => Boolean): LogRecordFilter = new LogRecordFilter {
    def f: LogRecord => Boolean = x
  }
}

object Test extends App {

  import LogRecord._

  val f1 = LogRecordFilter(l => if (l.id == "c1") true else false)
  val f2 = LogRecordFilter(l => if (l.id == "c2") true else false)
  val ft = LogRecordFilter(l => (100 to 200) contains (l.time))
  val fhier = HierLogRecordFilter("uvm_test_top.env")
  val fc1 = CompNameLogRecordFilter("c1")
  val fc2 = CompNameLogRecordFilter("c2")
  val ft1 = ft && f1
  val ftext = TextContainsLogRecordFilter("ee6")
  var lb: ListBuffer[LogRecord] = null

  //println(args(0))

  if (args.length > 0) {
    val uf = new UvmLogFilter(args(0))
    //lb = uf.getFiltered(f1)
    //fl.foreach(println)
    //println("f2 = ")
    //val fl2 = uf.getFiltered(f2)
    //fl2.foreach(println)
    //println("f_or = ")
    //val f_or = f1 || f2
    //val fl3 = uf.getFiltered(f_or)
    //fl3.foreach(println)
    //val fft = uf.getFiltered(ft)
    //fft.foreach(println)
    //lb = uf.getFiltered(ft1)
    //lb = uf.getFiltered(ftext)
    //lb = uf.getFiltered(fhier)
    lb = uf.getFiltered(ftext)
    lb.foreach(printRecord)
  }
  else {
    println("At least one argument required")
  }

}
