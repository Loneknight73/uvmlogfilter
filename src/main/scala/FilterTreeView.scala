package UvmLogFilterGUI

import scalafx.Includes._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, TreeItem, TreeView}
import uvmlog.{FilterExpr, FilterNode, LogRecordFilter, LogicalOpNode}

class FilterTreeView extends TreeView[FilterExpr] {

  var model: FilterExpr = null

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

  def getFilterFromDialog(): Option[FilterNode] = {
    val dialog = UvmLogFilterGUI.addFilterDialog()
    val x = dialog.showAndWait()
    x match {
      case None => None
      case Some(f: FilterNode) => Some(f)
    }
  }


  def getLogOpFromDialog(): Option[LogicalOpNode] = {
    val dialog = UvmLogFilterGUI.addLogicalOpDialog()
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
        val maybeFilter = getFilterFromDialog()
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

    val maybeLogOp = getLogOpFromDialog()
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
