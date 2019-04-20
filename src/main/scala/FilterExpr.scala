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

object LogicalOpNode {

  def add(p: LogicalOpNode, c: FilterExpr): Unit = {
    p.add(c)
  }

  def remove(p: LogicalOpNode, c: FilterExpr): Unit = {
    p.remove(c)
  }
}

case class FilterNode(f: LogRecordFilter) extends FilterExpr {
  override def toString: String = f.toString

  def eval(): Option[LogRecordFilter] = Some(f)
}