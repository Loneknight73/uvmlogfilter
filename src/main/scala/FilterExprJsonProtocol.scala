/*
    uvmlogfilter - A Scala program to filter UVM logs
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

package com.github.uvmlogfilter.model
import spray.json._

object FilterExprJsonProtocol extends DefaultJsonProtocol {

  implicit val idLogRecordFilterFormat: JsonFormat[IdLogRecordFilter] = jsonFormat2(IdLogRecordFilter)
  implicit val severityLogRecordFilterFormat: JsonFormat[SeverityLogRecordFilter] = jsonFormat1(SeverityLogRecordFilter)
  implicit val timeLogRecordFilterFormat: JsonFormat[TimeLogRecordFilter] = jsonFormat2(TimeLogRecordFilter)
  implicit val hierLogRecordFilterFormat: JsonFormat[HierLogRecordFilter] = jsonFormat1(HierLogRecordFilter)
  implicit val compNameLogRecordFilter: JsonFormat[CompNameLogRecordFilter] = jsonFormat(CompNameLogRecordFilter, "c")
  implicit val textContainsLogRecordFilter: JsonFormat[TextContainsLogRecordFilter] = jsonFormat1(TextContainsLogRecordFilter)

  implicit object LogRecordFilterFormat extends RootJsonFormat[LogRecordFilter] {
    override def write(f: LogRecordFilter): JsValue = {
      f match {
        case x: SeverityLogRecordFilter => JsObject("SeverityLogRecordFilter" -> x.toJson)
        case x: IdLogRecordFilter => JsObject("IdLogRecordFilter" -> x.toJson)
        case x: TimeLogRecordFilter => JsObject("TimeLogRecordFilter" -> x.toJson)
        case x: HierLogRecordFilter => JsObject("HierLogRecordFilter" -> x.toJson)
        case x: CompNameLogRecordFilter => JsObject("CompNameLogRecordFilter" -> x.toJson)
        case x: TextContainsLogRecordFilter => JsObject("TextContainsLogRecordFilter" -> x.toJson)
        case _ => serializationError("Error")
      }
    }

    override def read(json: JsValue): LogRecordFilter = {
      json match {
        case JsObject(map) => {
          val l = List("SeverityLogRecordFilter", "IdLogRecordFilter", "TimeLogRecordFilter",
            "HierLogRecordFilter", "CompNameLogRecordFilter", "TextContainsLogRecordFilter").
            map(i => map.contains(i))
          val i = l.indexOf(true)
          i match {
            case 0 => map("SeverityLogRecordFilter").convertTo[SeverityLogRecordFilter]
            case 1 => map("IdLogRecordFilter").convertTo[IdLogRecordFilter]
            case 2 => map("TimeLogRecordFilter").convertTo[TimeLogRecordFilter]
            case 3 => map("HierLogRecordFilter").convertTo[HierLogRecordFilter]
            case 4 => map("CompNameLogRecordFilter").convertTo[CompNameLogRecordFilter]
            case 5 => map("TextContainsLogRecordFilter").convertTo[TextContainsLogRecordFilter]
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
              val op: LogicalOpNode = LogicalOpNode(m("op").convertTo[String])
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
