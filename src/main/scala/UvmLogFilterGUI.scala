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

package UvmLogFilterGUI

import java.io.{File, FileWriter}

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
import scalafx.scene.text.Font
import scalafx.stage.FileChooser
import spray.json._
import uvmlog._
import uvmlog.FiterExprJsonProtocol._

import scala.io.Source

object UvmLogFilterGUI extends JFXApp {

  val statusArea = buildStatusArea()
  val addFilterButton = new Button("Add filter")
  val filterArea = new FilterTreeView()
  val buttonArea = buildButtonArea()
  val textArea = buildTextArea()
  val centerPane = buildCenter()
  var selectedLogFile: File = null
  var filtersFile: File = null
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
    val reloadItem = new MenuItem("Reload")
    reloadItem.onAction = (e: ActionEvent) => {
      reloadFile
    }

    val saveItem = new MenuItem("Save as...")
    openItem.accelerator = new KeyCodeCombination(KeyCode.S,
      KeyCombination.ControlDown)
    saveItem.onAction = (e: ActionEvent) => {
      val fileChooser = new FileChooser
      val selectedFile = fileChooser.showSaveDialog(stage)
      if (selectedFile != null) {
        val fileWriter = new FileWriter(selectedFile)
        fileWriter.write(textArea.getText)
        fileWriter.close()
      }

    }
    val exitItem = new MenuItem("Exit")
    exitItem.accelerator = new KeyCodeCombination(KeyCode.X,
      KeyCombination.ControlDown)
    exitItem.onAction = (e: ActionEvent) => {
      sys.exit(0)
    }

    fileMenu.items = List(openItem, reloadItem, saveItem, new SeparatorMenuItem, exitItem)

    val filtersMenu = new Menu("Filters")
    val loadFiltersItem = new MenuItem("Load...")
    loadFiltersItem.onAction = (e: ActionEvent) => {
      val fileChooser = new FileChooser
      val initialDir = new File(System.getProperty("user.dir"))
      fileChooser.setInitialDirectory(initialDir)
      val f = fileChooser.showOpenDialog(stage)
      if (f != null) {
        val x = deserializeFilters(f)
        filtersFile = f
        statusArea.setFilter(filtersFile.getAbsolutePath())
      }
    }

    val saveFiltersItem = new MenuItem("Save")
    saveFiltersItem.onAction = (e: ActionEvent) => {
      if (filtersFile != null) {
        val s = serializeFilters()
        s match {
          case None =>
          case Some(x) => {
            val fileWriter = new FileWriter(filtersFile)
            fileWriter.write(x)
            fileWriter.close()
          }
        }
      }
    }

    val saveAsFiltersItem = new MenuItem("Save as...")
    saveAsFiltersItem.onAction = (e: ActionEvent) => {
      val fileChooser = new FileChooser
      val initialDir = new File(System.getProperty("user.dir"))
      fileChooser.setInitialDirectory(initialDir)
      filtersFile = fileChooser.showSaveDialog(stage)
      if (filtersFile != null) {
        val s = serializeFilters()
        s match {
          case None =>
          case Some(x) => {
            val fileWriter = new FileWriter(filtersFile)
            fileWriter.write(x)
            fileWriter.close()
          }
        }

      }
    }
    filtersMenu.items = List(loadFiltersItem, saveFiltersItem, saveAsFiltersItem)

    menuBar.menus = List(fileMenu, filtersMenu)
    menuBar
  }

  def serializeFilters(): Option[String] = {
    val fexpr = filterArea.getModel()
    Option(fexpr) match {
      case None => {
        new Alert(AlertType.Error) {
          initOwner(stage)
          title = "Error"
          headerText = "Error"
          contentText = "Unable to parse filter expression"
        }.showAndWait()
        None
      }
      case Some(f) => {
        val s = f.toJson.sortedPrint
        Some(s)
      }
    }
  }

  def deserializeFilters(file: File) = {
    val json = Source.fromFile(file).mkString.parseJson
    val op = json.convertTo[FilterExpr]
    filterArea.setModel(op)
  }

  def openFile: Unit = {
    val fileChooser = new FileChooser
    val initialDir = new File(System.getProperty("user.dir"))
    fileChooser.setInitialDirectory(initialDir)
    selectedLogFile = fileChooser.showOpenDialog(stage)
    if (selectedLogFile != null) {
      statusArea.setLog(selectedLogFile.getAbsolutePath())
      uvmLogRec = new UvmLogFilter(selectedLogFile.getAbsolutePath())
      statusArea.setTotal(uvmLogRec.getRecordsNum())
    }
  }

  def reloadFile: Unit = {
    if (selectedLogFile != null) {
      statusArea.setLog(selectedLogFile.getAbsolutePath()) // useless?
      uvmLogRec = new UvmLogFilter(selectedLogFile.getAbsolutePath())
      statusArea.setTotal(uvmLogRec.getRecordsNum())
    }
  }

  def filterSpecificPane(t: String): FilterPane = {
    t match {
      case "Id" => new IdFilterPane(None)
      case "Severity" => new SeverityFilterPane(None)
      case "Time" => new TimeFilterPane(None)
      case "Hierarchy" => new HierFilterPane(None)
      case "Component" => new CompNameFilterPane(None)
      case "Text" => new TextContainsFilterPane(None)
    }
  }

  def addFilterDialog(): Dialog[FilterNode] = {

    val filtSpecPos = 2
    var filtSpecPane: Pane = null
    var fpane: FilterPane = null

    val dialog = new Dialog[FilterNode]() {
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
        FilterNode(fpane.getFilter())
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
    addLogicalOpButton.onAction = (ae: ActionEvent) => filterArea.addLogicalOp()
    val addFilterButton = new Button("Add filter")
    addFilterButton.onAction = (ae: ActionEvent) => filterArea.addFilter()
    val deleteSelectedButton = new Button("Delete selected")
    deleteSelectedButton.onAction = (ae: ActionEvent) => filterArea.deleteSelected()
    val h = new HBox {
      padding = Insets(5)
      spacing = 5
      children = List(applyFiltersButton, sep, addLogicalOpButton, addFilterButton, deleteSelectedButton)
    }
    h

  }

  def addLogicalOpDialog(): Dialog[LogicalOpNode] = {
    val dialog = new Dialog[LogicalOpNode]() {
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
        LogicalOpNode(opCombo.value.value)
      else
        null
    }
    dialog
  }

  def applyFilters() = {
    if (uvmLogRec != null) {
      val of = filterArea.getModel().eval()
      of match {
        case None => {
          new Alert(AlertType.Error) {
            initOwner(stage)
            title = "Error"
            headerText = "Error"
            contentText = "Unable to parse filter expression"
          }.showAndWait()
        }
        case Some(f) => {
          val lb = uvmLogRec.getFiltered(f)
          val lbs = for {
            lr <- lb
            l <- lr.message
          } yield (l)
          val text = lbs.mkString("\n")
          textArea.text = text
          statusArea.setFilt(lb.length)
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
    ta.setFont(Font.font("monospaced"))
    ta
  }

  def buildCenter(): Pane = {
    val pane = new VBox()
    val spane = new SplitPane()
    spane.items.addAll(filterArea, textArea)
    spane.setOrientation(Orientation.Vertical)
    spane.setDividerPositions(0.4)
    pane.spacing = 5
    pane.children = List(buttonArea, spane)
    pane
  }

  def buildStatusArea(): StatusView = {
    new StatusView()
  }

  stage = new JFXApp.PrimaryStage {

    title = "UVM Log Filter"
    scene = new Scene(600, 800) {
      val menuBar = buildMenuBar()

      borderPane.top = menuBar
      borderPane.bottom = statusArea
      borderPane.center = centerPane
      root = borderPane
    }
  }

}

