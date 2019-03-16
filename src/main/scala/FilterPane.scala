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

import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{ComboBox, Label, TextField}
import scalafx.scene.layout.{GridPane, Pane}
import uvmlog._

trait FilterPane {
  def getPane(): Pane
  def getFilter(): LogRecordFilter
}

class IdFilterPane(f: Option[IdLogRecordFilter]) extends FilterPane {

  val matchCombo = new ComboBox[String]() {
    items = ObservableBuffer("is", "contains")
    value = "contains"
  }
  val idText = new TextField() {
    promptText = "Id"
  }

  val pane = new GridPane() {

    padding = Insets(20, 100, 10, 10)

    add(new Label("Id contains:"), 0, 0)
    add(matchCombo, 1, 0)
    add(idText, 2, 0)
  }

  override def getPane(): Pane = {
    pane
  }

  override def getFilter(): LogRecordFilter = {
    IdLogRecordFilter(idText.text(), matchCombo.value.value)
  }
}

class SeverityFilterPane(f: Option[SeverityLogRecordFilter]) extends FilterPane {

  val label = new Label("Severity")

  val sevCombo = new ComboBox[String]() {
    items = ObservableBuffer("UVM_INFO", "UVM_WARNING", "UVM_ERROR", "UVM_FATAL")
    value = "UVM_INFO"
  }

  val pane = new GridPane() {
    hgap = 10
    vgap = 10

    padding = Insets(20, 100, 10, 10)
    add(label, 0, 0)
    add(sevCombo, 1, 0)
  }

  override def getPane(): Pane = {
    pane
  }

  override def getFilter(): LogRecordFilter = {
    SeverityLogRecordFilter(sevCombo.value.value)
  }
}

class TimeFilterPane(f: Option[TimeLogRecordFilter]) extends FilterPane {

  val label1 = new Label("Time inside")
  val minText = new TextField() {
    promptText = "min"
  }
  val label2 = new Label(":")
  val maxText = new TextField() {
    promptText = "max"
  }

  val pane = new GridPane() {
    hgap = 10
    vgap = 10

    add(label1, 0, 0)
    add(minText, 1, 0)
    add(label2, 2, 0)
    add(maxText, 3, 0)
  }

  override def getPane(): Pane = {
    pane
  }

  override def getFilter(): LogRecordFilter = {
    val min: BigInt = minText.text() match {
      case "" => 0
      case n: String => BigInt(n)
    }
    val max: BigInt = maxText.text() match {
      case "" => BigInt(2).pow(64)-1
      case n: String => BigInt(n)
    }
    TimeLogRecordFilter(min, max)
  }
}

class HierFilterPane(f: Option[HierLogRecordFilter]) extends FilterPane {

  val hierText = new TextField() {
    promptText = "hierarchy"
  }

  val pane = new GridPane() {

    add(new Label("Under hierarchy:"), 0, 0)
    add(hierText, 1, 0)
  }

  override def getPane(): Pane = {
    pane
  }

  override def getFilter(): LogRecordFilter = {
    HierLogRecordFilter(hierText.text())
  }
}

class CompNameFilterPane(f: Option[CompNameLogRecordFilter]) extends FilterPane {

  val compNameText = new TextField() {
    promptText = "Component"
  }

  val pane = new GridPane() {

    add(new Label("Component name is:"), 0, 0)
    add(compNameText, 1, 0)
  }

  override def getPane(): Pane = {
    pane
  }

  override def getFilter(): LogRecordFilter = {
    CompNameLogRecordFilter(compNameText.text())
  }
}

class TextContainsFilterPane(f: Option[TextContainsLogRecordFilter]) extends FilterPane {

  val textContainsText = new TextField() {
    promptText = "text"
  }

  val pane = new GridPane() {

    add(new Label("Text contains:"), 0, 0)
    add(textContainsText, 1, 0)
  }

  override def getPane(): Pane = {
    pane
  }

  override def getFilter(): LogRecordFilter = {
    TextContainsLogRecordFilter(textContainsText.text())
  }
}