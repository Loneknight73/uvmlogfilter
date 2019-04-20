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
    if (me.clickCount == 2) modifySelected()
  }

  def getModel: FilterExpr = model

  def makeTree(f: FilterExpr): TreeItem[FilterExpr] = {
    f match {
      case f: LogicalOpNode =>
        val item = new TreeItem[FilterExpr](f)
        item.setExpanded(true)
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

  def getParent(ti: TreeItem[FilterExpr]): (Option[(TreeItem[FilterExpr], FilterExpr)]) = {
    for {
      pti <- Option(ti.getParent())
      pfe <- Option(pti.getValue())
    } yield (pti, pfe)
  }

  def addTreeItem(p: TreeItem[FilterExpr], c: TreeItem[FilterExpr]): Unit = {
    p.getChildren().add(c)
  }

  def addTreeItem(p: TreeItem[FilterExpr], c: TreeItem[FilterExpr], before: TreeItem[FilterExpr]): Unit = {
    val i = p.getChildren().indexOf(before)
    p.getChildren().add(i, c)
  }


  def removeTreeItem(p: TreeItem[FilterExpr], c: TreeItem[FilterExpr]): Unit = {
    p.getChildren().remove(c)
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

  def addNode(maybeParent: Option[(TreeItem[FilterExpr], FilterExpr)], maybeFe: Option[FilterExpr]): Unit = {
    (maybeParent, maybeFe) match {
      case (_, None) => () // Nothing to add
      case (Some((pti, pfe: LogicalOpNode)), Some(fe)) => {
        // View
        val newti = new TreeItem(fe)
        newti.setExpanded(true)
        addTreeItem(pti, newti)
        // Model
        pfe.add(fe)
      }
      case (None, Some(fe: LogicalOpNode)) => {
        // View
        val newti = new TreeItem[FilterExpr](fe)
        newti.setExpanded(true)
        this.setRoot(newti)
        // Model
        model = fe
      }
      case (None, Some(fe: FilterNode)) => {
        new Alert(AlertType.Error) {
          initOwner(UvmLogFilterGUI.stage)
          title = "Error"
          headerText = "Error"
          contentText = "Filters cannot be root of the expression"
        }.showAndWait()
      }
      case _ => () // Bug
    }
  }

  def getParentLogOp(maybeTi: Option[TreeItem[FilterExpr]]): Option[(TreeItem[FilterExpr], FilterExpr)] = {
    maybeTi match {
      // If nothing selected, add to the root
      // If there is no root, return None, otherwise Some(root)
      case None => {
        Option(this.getRoot()) match {
          case Some(root) => Some(root, model)
          case None => None
        }
      }
      // Otherwise, if it's a LogicalOpNode, add to it,
      // If it's a FilterNode, add to its parent
      case Some(ti) => {
        ti.getValue() match {
          case op: LogicalOpNode => Some((ti, op))
          case f: FilterNode => Some(ti.getParent(), ti.getParent.getValue)
        }
      }
    }
  }

  def addFilter(): Unit = {
    val s: Option[TreeItem[FilterExpr]] = Option(this.getSelectionModel().getSelectedItem())
    val maybeParent = getParentLogOp(s)
    val maybeFilt = getFilterFromDialog(None)
    addNode(maybeParent, maybeFilt)
  }

  def addLogicalOp(): Unit = {
    val s: Option[TreeItem[FilterExpr]] = Option(this.getSelectionModel().getSelectedItem())
    val maybeParent = getParentLogOp(s)
    val maybeOp = getLogOpFromDialog(None)
    addNode(maybeParent, maybeOp)
  }

  def substituteOp(oldti: TreeItem[FilterExpr], newop: LogicalOpNode): Unit = {
    val x = getParent(oldti)
    x match {
      case Some((pti, pfe: LogicalOpNode)) => {
        // View
        val newti = new TreeItem[FilterExpr](newop)
        newti.setExpanded(true)
        newti.children.addAll(oldti.getChildren)
        addTreeItem(pti, newti, oldti)
        removeTreeItem(pti, oldti)
        // Model
        val oldfe = oldti.getValue
        oldfe match {
          case oldop: LogicalOpNode => {
            oldop.children.map(c => newop.add(c))
            pfe.remove(oldop)
            pfe.add(newop)
          }
          case _ => () // Bug
        }
      }
      case None => {
        val newti = new TreeItem[FilterExpr](newop)
        newti.children.addAll(oldti.getChildren)
        newti.setExpanded(true)
        this.setRoot(newti)
        // Model
        val oldfe = oldti.getValue
        oldfe match {
          case oldop: LogicalOpNode => {}
            oldop.children.map(c => newop.add(c))
            model = newop
          case _ => () //Bug
        }
      }
    }
  }

  def substituteFn(oldti: TreeItem[FilterExpr], newfn: FilterNode) = {
    val x = getParent(oldti)
    x match {
      case Some((pti, pfe: LogicalOpNode)) => {
        // View
        val newfti = new TreeItem[FilterExpr](newfn)
        newfti.setExpanded(true)
        addTreeItem(pti, newfti, oldti)
        removeTreeItem(pti, oldti)
        // Model
        val oldfe = oldti.getValue
        LogicalOpNode.remove(pfe, oldfe)
        LogicalOpNode.add(pfe, newfn)
      }
      case _ => () // Bug
    }
  }

  def modifyNode(ti: TreeItem[FilterExpr]): Unit = {
    ti.getValue() match {
      case lop: LogicalOpNode => {
        for {
          newOp <- getLogOpFromDialog(Some(lop))
        } yield substituteOp(ti, newOp)
      }
      case fn: FilterNode => {
        for {
          newFn <- getFilterFromDialog(Some(fn))
        } yield substituteFn(ti, newFn)
      }
    }
  }

  def modifySelected() = {
    for {
      s <- Option(this.getSelectionModel().getSelectedItem())
    } yield modifyNode(s)
  }

  def deleteNode(ti: TreeItem[FilterExpr]): Unit = {
    val x = getParent(ti)
    x match {
      case None => {
        this.setRoot(null)
        model = null
      }
      case Some((pti, pfe: LogicalOpNode)) => {
        removeTreeItem(pti, ti)
        val fe = ti.getValue
        pfe.remove(fe)
      }
      case _ => () // Bug
    }
  }

  def deleteSelected() = {
    for {
      s <- Option(this.getSelectionModel().getSelectedItem())
    } yield deleteNode(s)
  }

}
