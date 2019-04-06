package uvmlog

import spray.json._
import uvmlog._

object FiterExprJsonProtocol extends DefaultJsonProtocol {

  implicit val idLogRecordFilterFormat: JsonFormat[IdLogRecordFilter] = jsonFormat2(IdLogRecordFilter)
  implicit val severityLogRecordFilterFormat: JsonFormat[SeverityLogRecordFilter] = jsonFormat1(SeverityLogRecordFilter)
  implicit val timeLogRecordFilterFormat: JsonFormat[TimeLogRecordFilter] = jsonFormat2(TimeLogRecordFilter)

  implicit object LogRecordFilterFormat extends RootJsonFormat[LogRecordFilter] {
    override def write(f: LogRecordFilter): JsValue = {
      f match {
        case x: SeverityLogRecordFilter => JsObject("SeverityLogRecordFilter" -> x.toJson)
        case x: IdLogRecordFilter => JsObject("IdLogRecordFilter" -> x.toJson)
        case x: TimeLogRecordFilter => JsObject("TimeLogRecordFilter" -> x.toJson)
        case _ => JsNumber(1)
      }
    }

    override def read(json: JsValue): LogRecordFilter = {
      json match {
        case JsObject(map) => {
          val l = List("SeverityLogRecordFilter", "IdLogRecordFilter", "TimeLogRecordFilter").
            map(i => map.contains(i))
          val i = l.indexOf(true)
          i match {
            case 0 => map("SeverityLogRecordFilter").convertTo[SeverityLogRecordFilter]
            case 1 => map("IdLogRecordFilter").convertTo[IdLogRecordFilter]
            case 2 => map("TimeLogRecordFilter").convertTo[TimeLogRecordFilter]
            case _ => deserializationError("Error")
          }
        }
      }
    }
  }

  implicit val filterExprFormat: JsonFormat[FilterExpr] = lazyFormat(FilterExprFormat)

  implicit object LogicalOpNodeFormat extends RootJsonFormat[LogicalOpNode] {
    override def write(obj: LogicalOpNode): JsValue = {
      val jsv = obj.children.toList.map(x => x.toJson).toVector
      JsObject("LogicalOp" -> JsObject(
        "op" -> JsString(obj.op),
        "children" -> JsArray(jsv))
      )
    }

    override def read(json: JsValue): LogicalOpNode = {
      val lop = json.asJsObject().fields("LogicalOp")
      lop match {
        case JsObject(m) => {
          List("op", "children").map(i => m.contains(i)).toArray match {
            case Array(true, true) => {
              val op: LogicalOpNode = LogicalOpNode(m("op").toString())
              val children = m("children")
              children match {
                case JsArray(jsv) => {
                  val xs = jsv.map(x => x.convertTo[FilterExpr])
                  xs.map(x => op.add(x))
                  op
                }
                case _ => deserializationError("Error")
              }
            }
          }
        }
        case _ => deserializationError("Error")
      }
    }

  }

  implicit object FilterNodeFormat extends RootJsonFormat[FilterNode] {
    override def write(obj: FilterNode): JsValue = {
      obj.f.toJson
    }

    override def read(json: JsValue): FilterNode = {
      json match {
        case x: JsObject => {
          FilterNode(x.convertTo[LogRecordFilter])
        }
        case _ => deserializationError("Error")
      }
    }
  }

  implicit object FilterExprFormat extends RootJsonFormat[FilterExpr] {
    override def write(f: FilterExpr): JsValue = f match {
      case op: LogicalOpNode => op.toJson
      case f: FilterNode => f.toJson
    }

    override def read(json: JsValue): FilterExpr = {
      json match {
        case x: JsObject => {
          if (x.fields.contains("LogicalOp")) {
            json.convertTo[LogicalOpNode]
          }
          else {
            json.convertTo[FilterNode]
          }
        }
        case _ => deserializationError("Error")
      }
    }
  }

}
