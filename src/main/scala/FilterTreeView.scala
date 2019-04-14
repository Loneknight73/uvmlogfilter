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

package com.github.uvmlogfilter.gui

import com.github.uvmlogfilter.model._
import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, TreeItem, TreeView}
import scalafx.scene.input.MouseEvent

class FilterTreeView extends TreeView[FilterExpr] {

  var model: FilterExpr = null

  this.onMouseClicked = (me: MouseEvent) => {
    if (me.clickCount == 2) modifySelected()
  }

  def getModel(): FilterExpr = model

  def makeTree(f: FilterExpr): TreeItem[FilterExpr] = {
    f match {
      case f: LogicalOpNode =>
        val item = new TreeItem[FilterExpr](f)
        item.children = f.children.map(c => makeTree(c))
        item
      case _ => new TreeItem(f)
    }
  }

  def setModel(root: FilterExpr) = {
    val ti = makeTree(root)
    ti.setExpanded(true)
    this.setRoot(ti)
    model = root
  }

  def getNearestLogOp(ti: TreeItem[FilterExpr]): Option[TreeItem[FilterExpr]] = {
    // If nothing selected, add to the root
    if (ti == null) {
      val root = this.getRoot()
      // If there is no root, return None, otherwise Some(root)
      Option(root)
    }
    // Otherwise, if it's a LogicalOpNode, add to it,
    // If it's a FilterNode, add to its parent
    else {
      val fexpr = ti.getValue()
      fexpr match {
        case LogicalOpNode(_) => Some(ti)
        case FilterNode(_) => Some(ti.getParent())
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
    val addTo = getNearestLogOp(selected)
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

  def addLogicalOp(): Unit = {
    // Logical Ops can be added only to an empty tree (they become the root)
    // or to another logical Op
    val selected = this.getSelectionModel().getSelectedItem()

    val maybeLogOp = getLogOpFromDialog(None)
    maybeLogOp match {
      case None => ()
      case Some(newOp) => {
        val addTo = getNearestLogOp(selected)
        addTo match {
          // Root
          case None => {
            val ti: TreeItem[FilterExpr] = new TreeItem(newOp)
            ti.setExpanded(true)
            this.setRoot(ti)
            model = newOp
          }
          case Some(parent) => {
            val fti: TreeItem[FilterExpr] = new TreeItem(newOp)
            fti.setExpanded(true)
            parent.getChildren().add(fti)
            val p = parent.getValue()
            p match {
              case op: LogicalOpNode => op.add(newOp)
              case _ => () // Should never happen
            }
          }
        }
      }
    }
  }

  def substituteOp(oldfti: TreeItem[FilterExpr], newOp: LogicalOpNode) = {
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
    val addTo = getNearestLogOp(oldfti)
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

  def modifySelected() = {
    val selected = this.getSelectionModel().getSelectedItem()
    if (selected != null) {
      selected.getValue() match {
        case lop: LogicalOpNode => {
          val maybeLogOp = getLogOpFromDialog(Some(lop))
          maybeLogOp match {
            case None => ()
            case Some(newOp) => substituteOp(selected, newOp)
          }
        }
        case fn: FilterNode => {
          val maybeFiltNode = getFilterFromDialog(Some(fn))
          maybeFiltNode match {
            case None => ()
            case Some(newFn) => substituteFn(selected, newFn)
          }
        }
      }
    }
  }


  def deleteSelected() = {
    val selected = this.getSelectionModel().getSelectedItem()
    if (selected != null) {
      val parent = selected.getParent
      if (parent != null) {
        parent.getChildren.remove(selected)
        val x = parent.getValue()
        x match {
          case op: LogicalOpNode => op.remove(selected.getValue())
          case _ => () // Should never happen
        }
      }
      else { // The root was selected
        this.setRoot(null)
        model = null
      }
    }
  }

}
