package UvmLogFilterGUI

import java.io.FileWriter

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.geometry.{Insets, Orientation}
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.ButtonBar.ButtonData
import scalafx.scene.control._
import scalafx.scene.input._
import scalafx.scene.layout._
import scalafx.stage.FileChooser
import uvmlog._

sealed trait FilterTreeItem

case class LogicalOp(op: String) extends FilterTreeItem {
  override def toString() = op
}

case class Filter(f: LogRecordFilter) extends FilterTreeItem {
  override def toString: String = f.toString
}

object UvmLogFilterGUI extends JFXApp {

  val statusLabel = new Label("No file selected")
  val addFilterButton = new Button("Add filter")
  val filterArea = buildFilterArea()
  val buttonArea = buildButtonArea()
  val textArea = buildTextArea()
  val centerPane = buildCenter()

  val borderPane = new BorderPane
  var uvmLogRec: UvmLogFilter = null

  def buildMenuBar(): MenuBar = {
    val menuBar = new MenuBar()
    val fileMenu = new Menu("File")
    val openItem = new MenuItem("Open")
    openItem.accelerator = new KeyCodeCombination(KeyCode.O,
      KeyCombination.ControlDown)
    openItem.onAction = (e: ActionEvent) => {
      openFile
    }
    val saveItem = new MenuItem("Save as...")
    openItem.accelerator = new KeyCodeCombination(KeyCode.S,
      KeyCombination.ControlDown)
    saveItem.onAction = (e:ActionEvent) => {
      val fileChooser = new FileChooser
      val selectedFile = fileChooser.showSaveDialog(stage)
      if (selectedFile != null) {
        val fileWriter = new FileWriter(selectedFile)
        fileWriter.write(textArea.getText)
      }

    }
    val exitItem = new MenuItem("Exit")
    exitItem.accelerator = new KeyCodeCombination(KeyCode.X,
      KeyCombination.ControlDown)
    exitItem.onAction = (e: ActionEvent) => {
      sys.exit(0)
    }

    fileMenu.items = List(openItem, saveItem, new SeparatorMenuItem, exitItem)
    menuBar.menus = List(fileMenu)
    menuBar
  }

  def openFile: Unit = {
    val fileChooser = new FileChooser
    val selectedFile = fileChooser.showOpenDialog(stage)
    if (selectedFile != null) {
      statusLabel.text = "" + selectedFile
      uvmLogRec = new UvmLogFilter(selectedFile.getAbsolutePath())
    }
  }

  def buildFilterArea() = {

    val tv = new TreeView[FilterTreeItem]()
    tv
  }

  def filterSpecificPane (t: String): FilterPane = {
    t match {
      case "Id" => new IdFilterPane(None)
      case "Severity" => new SeverityFilterPane(None)
      case "Time" => new TimeFilterPane(None)
      case "Hierarchy" => new HierFilterPane(None)
      case "Component" => new CompNameFilterPane(None)
      case "Text" => new TextContainsFilterPane(None)
    }
  }

  def addFilterDialog(): Dialog[LogRecordFilter] = {

    val filtSpecPos = 2
    var filtSpecPane: Pane = null
    var fpane: FilterPane = null

    val dialog = new Dialog[LogRecordFilter]() {
      title = "Create Filter"
    }
    val okButton = new ButtonType("Ok", ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(okButton, ButtonType.Cancel)

    val filterTypeCombo = new ComboBox[String]() {
      items = ObservableBuffer("Id", "Severity", "Time", "Hierarchy", "Component", "Text")
      value = "Id"
    }

    dialog.resultConverter = { dialogButton =>
      if (dialogButton == okButton)
         fpane.getFilter()
      else
        null
    }

    val grid = new GridPane() {
      hgap = 10
      vgap = 10
      padding = Insets(20, 100, 10, 10)

      add(new Label("Filter type: "), 0, 0)
      add(filterTypeCombo, 1, 0)
      fpane = filterSpecificPane(filterTypeCombo.value.value)
      filtSpecPane = fpane.getPane()
      add(filtSpecPane, filtSpecPos, 0)
    }

    filterTypeCombo.onAction = (e: ActionEvent) => {
      fpane = filterSpecificPane(filterTypeCombo.selectionModel.value.selectedItem.value)
      grid.getChildren.remove(filtSpecPane)
      filtSpecPane = fpane.getPane()
      grid.add(filtSpecPane, filtSpecPos, 0)
    }

    dialog.dialogPane().content = grid

    dialog
  }

  def buildButtonArea() = {
    val applyFiltersButton = new Button("Apply filters")
    applyFiltersButton.onAction = (ae: ActionEvent) => applyFilters()
    val sep: Separator = new Separator()
    sep.setOrientation(Orientation.Vertical)
    val addLogicalOpButton = new Button("Add logical op")
    addLogicalOpButton.onAction = (ae: ActionEvent) => addLogicalOp()
    val addFilterButton = new Button("Add filter")
    addFilterButton.onAction = (ae: ActionEvent) => addFilter()
    val deleteSelectedButton = new Button("Delete selected")
    deleteSelectedButton.onAction = (ae: ActionEvent) => deleteSelected()
    val h = new HBox {
      padding = Insets(5)
      spacing = 5
      children = List(applyFiltersButton, sep, addLogicalOpButton, addFilterButton, deleteSelectedButton)
    }
    h

  }

  def addFilter() = {
    val sel = filterArea.getSelectionModel().getSelectedItem()
    if (sel != null) {
      sel.getValue() match {
        case op: LogicalOp => {
          val dialog = addFilterDialog()
          val lrf = dialog.showAndWait()
          lrf match {
            case Some(l: LogRecordFilter) => {
              val fti: FilterTreeItem = Filter(l)
              sel.getChildren().add(new TreeItem(fti))
            }
            case _ => ()
          }
        }
        case f: Filter => ()
      }
    }
  }


  def addLogicalOp() = {
    // Logical Ops can be added only to an empty tree (they become the root)
    // or to another logical Op
    val sel = filterArea.getSelectionModel().getSelectedItem()
    val isTreeEmpty = filterArea.getRoot()
    (sel, isTreeEmpty) match {
      // Empty tree: logical op becomes the root
      case (_, null) => {
        val dialog = addLogicalOpDialog()
        val op = dialog.showAndWait()
        op match {
          case None => ()
          case Some(lop: LogicalOp) => {
            val fti: FilterTreeItem = lop
            val ti = new TreeItem(fti)
            ti.setExpanded(true)
            filterArea.setRoot(ti)
          }
        }
      }
      // Non empty tree, nothing selected: do nothing
      case (null, _) => ()
      // Something actually selected
      case (ti, _) => {
        val dialog = addLogicalOpDialog()
        val op = dialog.showAndWait()
        op match {
          case None => ()
          case Some(lop: LogicalOp) => {
            val fti: TreeItem[FilterTreeItem] = new TreeItem(lop)
            fti.setExpanded(true)
            // TODO: match on ti to add only if sel item is a LogicalOp
            ti.getChildren().add(fti)
          }
        }
      }
    }

  }

  def addLogicalOpDialog(): Dialog[LogicalOp] = {
    val dialog = new Dialog[LogicalOp]() {
      title = "Add Logical Op"
    }
    val okButton = new ButtonType("Ok", ButtonData.OKDone)
    dialog.dialogPane().buttonTypes = Seq(okButton, ButtonType.Cancel)

    val opCombo = new ComboBox[String]() {
      items = ObservableBuffer("AND", "OR", "NOT")
      value = "AND"
    }

    val grid = new GridPane() {
      hgap = 10
      vgap = 10
      padding = Insets(20, 100, 10, 10)

      add(new Label("Logical op:"), 0, 0)
      add(opCombo, 1, 0)
    }

    dialog.dialogPane().content = grid

    dialog.resultConverter = { dialogButton =>
      if (dialogButton == okButton)
        LogicalOp(opCombo.value.value)
      else
        null
    }
    dialog
  }

  def deleteSelected() = {
    val sel = filterArea.getSelectionModel().getSelectedItem()
    if (sel != null) {
      if (sel.getParent != null) {
        sel.getParent().getChildren.remove(sel)
      }
      else { // It should be the root
        filterArea.setRoot(null)
      }
    }
  }

  def getFilterFromTree(root: TreeItem[FilterTreeItem]): Option[LogRecordFilter] = {
    root.getValue() match {
      case op: LogicalOp => {
        val children = root.getChildren().map(ti => getFilterFromTree(ti)).toList
        op.op match {
          case "AND" => {
            val t: Option[LogRecordFilter] = Some(TrueLogRecordFilter())
            val res: Option[LogRecordFilter] = children.foldLeft(t)((of1 , of2) => {
              (of1, of2) match {
                case (Some(f1), Some(f2)) => Some (f1 && f2)
                case (_, _) => None
              }
            })
            res
          }
          case "OR"  => {
            val f: Option[LogRecordFilter] = Some(FalseLogRecordFilter())
            val res: Option[LogRecordFilter] = children.foldLeft(f)((of1 , of2) => {
              (of1, of2) match {
                case (Some(f1), Some(f2)) => Some (f1 || f2)
                case (_, _) => None
              }
            })
            res
          }
          case "NOT" => None
        }
      }
      case f: Filter => Some(f.f)
    }
  }

  def applyFilters() = {
    if (uvmLogRec != null) {
      val of = getFilterFromTree(filterArea.getRoot())
      of match {
        case None => ()
        case Some(f) => {
          val lb = uvmLogRec.getFiltered(f)
          val lbs = for {
            lr <- lb
            l <- lr.message
          } yield (l)
          val text = lbs.mkString("\n")
          textArea.text = text
        }
      }
    }
    else {
      new Alert(AlertType.Error) {
        initOwner(stage)
        title = "Error"
        headerText = "Error"
        contentText = "Must open a file before applying filters"
      }.showAndWait()
    }
  }


  def buildTextArea() = {
    val ta = new TextArea()
    ta.setEditable(false)
    ta.prefWidth = 100
    ta.prefHeight = 400
    ta
  }

  def buildCenter(): Pane = {
    val pane = new VBox()
    pane.spacing = 5
    pane.children = List(buttonArea, filterArea, textArea)
    pane
  }

  stage = new JFXApp.PrimaryStage {

    title = "UVM Log Filter"
    scene = new Scene(600, 800) {
      val menuBar = buildMenuBar()

      borderPane.top = menuBar
      borderPane.bottom = statusLabel
      borderPane.center = centerPane
      root = borderPane
    }
  }

}

