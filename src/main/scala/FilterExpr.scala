package uvmlog

import spray.json._
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

object FiterExprJsonProtocol extends DefaultJsonProtocol {

  implicit val idLogRecordFilterFormat: JsonFormat[IdLogRecordFilter] = jsonFormat2(IdLogRecordFilter)
  implicit object LogRecordFilterFormat extends RootJsonFormat[LogRecordFilter] {
    def write(f: LogRecordFilter): JsValue = {
      f match {
        case i: IdLogRecordFilter => JsObject("IdLogRecordFilter" -> i.toJson)
        case _ => JsNumber(1)
      }

    }
    override def read(json: JsValue): LogRecordFilter = {
      null
    }
  }

  implicit val filterExprFormat: JsonFormat[FilterExpr] = lazyFormat(FilterExprFormat)
  implicit object LogicalOpFormat extends RootJsonFormat[LogicalOpNode] {
    override def write(obj: LogicalOpNode): JsValue = {
      val jsv = obj.children.toList.map(x => x.toJson).toVector
      JsObject("LogicalOp" -> JsObject(
        "op" -> JsString(obj.op),
        "children" -> JsArray(jsv))
      )
    }

    override def read(json: JsValue): LogicalOpNode = {
      null
    }
  }

  implicit object FilterNodeFormat extends RootJsonFormat[FilterNode] {
    override def write(obj: FilterNode): JsValue = {
      obj.f.toJson
    }

    override def read(json: JsValue): FilterNode = ???
  }

  implicit object FilterExprFormat extends RootJsonFormat[FilterExpr] {
    override def write(f: FilterExpr): JsValue = f match {
      case op: LogicalOpNode => op.toJson
      case f: FilterNode => f.toJson
    }

    override def read(json: JsValue): FilterExpr = {
      null
    }
  }

}