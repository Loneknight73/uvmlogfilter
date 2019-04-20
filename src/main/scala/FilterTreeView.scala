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

package com.github.uvmlogfilter.gui

import com.github.uvmlogfilter.model._
import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, TreeItem, TreeView}
import scalafx.scene.input.MouseEvent

class FilterTreeView extends TreeView[FilterExpr] {

  var model: FilterExpr = _

  this.onMouseClicked = (me: MouseEvent) => {
    if (me.clickCount == 2) modifySelected2()
  }

  def getModel: FilterExpr = model

  def makeTree(f: FilterExpr): TreeItem[FilterExpr] = {
    f match {
      case f: LogicalOpNode =>
        val item = new TreeItem[FilterExpr](f)
        item.children = f.children.map(c => makeTree(c))
        item
      case _ => new TreeItem(f)
    }
  }

  def setModel(root: FilterExpr): Unit = {
    val ti = makeTree(root)
    ti.setExpanded(true)
    this.setRoot(ti)
    model = root
  }

  def getNearestLogOp(maybeTi: Option[TreeItem[FilterExpr]]): Option[TreeItem[FilterExpr]] = {
    maybeTi match {
      // If nothing selected, add to the root
      // If there is no root, return None, otherwise Some(root)
      case None => Option(this.getRoot())
      // Otherwise, if it's a LogicalOpNode, add to it,
      // If it's a FilterNode, add to its parent
      case Some(ti) => {
        val fexpr = ti.getValue()
        fexpr match {
          case LogicalOpNode(_) => Some(ti)
          case FilterNode(_) => Some(ti.getParent())
        }
      }
    }
  }

  def getFilterFromDialog(fn: Option[FilterNode]): Option[FilterNode] = {
    val dialog = UvmLogFilterGUI.addFilterDialog(fn)
    val x = dialog.showAndWait()
    x match {
      case None => None
      case Some(f: FilterNode) => Some(f)
    }
  }


  def getLogOpFromDialog(lop: Option[LogicalOpNode]): Option[LogicalOpNode] = {
    val dialog = UvmLogFilterGUI.addLogicalOpDialog(lop)
    val x = dialog.showAndWait()
    x match {
      case None => None
      case Some(l: LogicalOpNode) => Some(l)
    }
  }

  def addFilter(): Unit = {
    val selected = this.getSelectionModel().getSelectedItem()
    val addTo = getNearestLogOp(Option(selected))
    addTo match {
      // A FilterNode cannot be the root of a FilterExpr
      case None => {
        new Alert(AlertType.Error) {
          initOwner(UvmLogFilterGUI.stage)
          title = "Error"
          headerText = "Error"
          contentText = "Filters cannot be root of the expression"
        }.showAndWait()
      }
      case Some(parent) => {
        val maybeFilter = getFilterFromDialog(None)
        maybeFilter match {
          case None => ()
          case Some(newFilt) => {
            val fti: TreeItem[FilterExpr] = new TreeItem(newFilt)
            fti.setExpanded(true)
            parent.getChildren().add(fti)
            val p = parent.getValue()
            p match {
              case op: LogicalOpNode => op.add(newFilt)
              case _ => () // Should never happen. TODO: throw exception
            }
          }

        }
      }
    }
  }

  def addNode(maybeParent: Option[TreeItem[FilterExpr]], maybeFexp: Option[FilterExpr]): Unit = {
    (maybeParent, maybeFexp) match {
      case (_, None) => ()
      case (Some(parentTi), Some(fexp)) => {
        // Model
        val parentFExp = parentTi.getValue
        parentFExp match {
          case op: LogicalOpNode => op.add(fexp)
          case _ => ()
        }
        // View
        val newTi = new TreeItem(fexp)
        newTi.setExpanded(true)
        parentTi.getChildren.add(newTi)
      }
      case (None, Some(fexp)) => {
        // Model
        model = fexp
        // View
        val newti = new TreeItem(fexp)
        newti.setExpanded(true)
        this.setRoot(newti)
      }
    }
  }

  def addLogicalOp2(): Unit = {
    val s: Option[TreeItem[FilterExpr]] = Option(this.getSelectionModel().getSelectedItem())
    val addTo = getNearestLogOp(s)
    val maybeLogOp = getLogOpFromDialog(None)
    addNode(addTo, maybeLogOp)
  }

  def substituteOp2(oldfti: TreeItem[FilterExpr], newOp: LogicalOpNode): Unit = {
    val maybeParent = Option(oldfti.getParent())
    val oldOp = oldfti.value()
    oldOp match {
      case lop: LogicalOpNode => {
        maybeParent match {
          case Some(parent) => {
            val parentOp = parent.getValue
            parentOp match {
              case l: LogicalOpNode => {
                lop.children.map(c => newOp.add(c))
                l.remove(oldOp)
                l.add(newOp)
                // TreeItem
                val newfti: TreeItem[FilterExpr] = new TreeItem(newOp)
                newfti.children.addAll(oldfti.getChildren)
                newfti.setExpanded(true)
                parent.getChildren().remove(oldfti)
                parent.getChildren().add(newfti)
              }
              case _ => ()
            }
          }
          // Root
          case None => {
            lop.children.map(c => newOp.add(c))
            val newfti: TreeItem[FilterExpr] = new TreeItem(newOp)
            newfti.children.addAll(oldfti.getChildren)
            newfti.setExpanded(true)
            this.setRoot(newfti)
            model = newOp
          }
        }
      }
      case _ => ()
    }
  }


  def substituteOp(oldfti: TreeItem[FilterExpr], newOp: LogicalOpNode): Unit = {
    val parent = oldfti.getParent()
    if (parent != null) {
      val oldOp = oldfti.value()
      oldOp match {
        case lop: LogicalOpNode => {
          // FilterExpr
          val parentOp = parent.getValue
          parentOp match {
            case l: LogicalOpNode => {
              lop.children.map(c => newOp.add(c))
              l.remove(oldOp)
              l.add(newOp)
              // TreeItem
              val newfti: TreeItem[FilterExpr] = new TreeItem(newOp)
              newfti.children.addAll(oldfti.getChildren)
              newfti.setExpanded(true)
              parent.getChildren().add(newfti)
              parent.getChildren().remove(oldfti)
            }
            case _ => ()
          }
        }
        case _ => ()
      }
    }
    else { // Root
      val oldOp = oldfti.value()
      oldOp match {
        case lop: LogicalOpNode => {
          lop.children.map(c => newOp.add(c))
          val newfti: TreeItem[FilterExpr] = new TreeItem(newOp)
          newfti.children.addAll(oldfti.getChildren)
          newfti.setExpanded(true)
          this.setRoot(newfti)
          model = newOp
        }
        case _ => ()
      }
    }
  }


  def substituteFn(oldfti: TreeItem[FilterExpr], newFn: FilterNode) = {
    val addTo = getNearestLogOp(Option(oldfti))
    addTo match {
      case None => () // TODO: bug
      case Some(parent) => {
        val newfti: TreeItem[FilterExpr] = new TreeItem(newFn)
        newfti.setExpanded(true)
        parent.getChildren().remove(oldfti)
        parent.getChildren.add(newfti)
        val p = parent.value()
        p match {
          case op: LogicalOpNode => {
            op.remove(oldfti.getValue())
            op.add(newFn)
          }
        }
      }
    }
  }

  def modifyNode(ti: TreeItem[FilterExpr]): Unit = {
    ti.getValue() match {
      case lop: LogicalOpNode => {
        for {
          newOp <- getLogOpFromDialog(Some(lop))
        } yield substituteOp2(ti, newOp)
      }
      case fn: FilterNode => {
        for {
          newFn <- getFilterFromDialog(Some(fn))
        } yield substituteFn(ti, newFn)
      }
    }
  }

  def modifySelected2() = {
    for {
      s <- Option(this.getSelectionModel().getSelectedItem())
    } yield modifyNode(s)
  }

  def deleteNode(ti: TreeItem[FilterExpr]): Unit = {
    val maybeParent = Option(ti.getParent)
    maybeParent match {
      case Some(parentTi) => {
        // Model
        val parentFExp = parentTi.getValue
        val fe = ti.getValue
        parentFExp match {
          case op: LogicalOpNode => op.remove(fe)
          case _ => ()
        }
        // View
        parentTi.getChildren.remove(ti)
      }
      case None => {
        this.setRoot(null)
        model = null
      }
    }
  }

  def deleteSelected2() = {
    for {
      s <- Option(this.getSelectionModel().getSelectedItem())
    } yield deleteNode(s)
  }

}
