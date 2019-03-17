package uvmlog

import uvmlog.LogRecordFilter
import scala.collection.mutable.ListBuffer

sealed trait FilterExpr {
  def eval(): Option[LogRecordFilter]
}

case class LogicalOpNode(op: String) extends FilterExpr {
  val children = ListBuffer[FilterExpr]()

  override def toString() = op

  def add(e: FilterExpr) = {
    children += e
  }

  def remove(e: FilterExpr) = {
    children -= e
  }

  def andCombine(f1: Option[LogRecordFilter], f2: Option[LogRecordFilter]): Option[LogRecordFilter] = {
    (f1, f2) match {
      case (Some(x1), Some(x2)) => Some(x1 && x2)
      case (_, _) => None
    }
  }

  def orCombine(f1: Option[LogRecordFilter], f2: Option[LogRecordFilter]): Option[LogRecordFilter] = {
    (f1, f2) match {
      case (Some(x1), Some(x2)) => Some(x1 || x2)
      case (_, _) => None
    }
  }

  def eval(): Option[LogRecordFilter] = {
    val subExpr = children.map(c => c.eval())
    if (subExpr.length == 0) {
      None
    }
    else {
      op match {
        case "AND" => {
          val t: Option[LogRecordFilter] = Some(TrueLogRecordFilter())
          subExpr.foldLeft(t)(andCombine(_, _))
        }
        case "OR" => {
          val f: Option[LogRecordFilter] = Some(FalseLogRecordFilter())
          subExpr.foldLeft(f)(orCombine(_, _))
        }
        case "NOT" => {
          if (subExpr.length != 1) {
            None
          }
          else {
            subExpr.head match {
              case None => None
              case Some(lrf) => Some(lrf.negate())
            }
          }
        }
      }
    }
  }
}

case class FilterNode(f: LogRecordFilter) extends FilterExpr {
  override def toString: String = f.toString

  def eval(): Option[LogRecordFilter] = Some(f)
}

