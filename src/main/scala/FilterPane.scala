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

import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.{ComboBox, Label, TextField}
import scalafx.scene.layout.{GridPane, Pane}
import com.github.uvmlogfilter.model._

trait FilterPane {
  def getPane(): Pane
  def getFilter(): LogRecordFilter
}

class IdFilterPane(of: Option[LogRecordFilter]) extends FilterPane {

  val (initMatchType, initId) = of match {
    case Some(f: IdLogRecordFilter) => (f.matchtype, f.s)
    case None => ("contains", "")
  }

  val matchCombo = new ComboBox[String]() {
    items = ObservableBuffer("is", "contains")
    value = initMatchType
  }
  val idText = new TextField() {
    promptText = "Id"
    text = initId
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

class SeverityFilterPane(of: Option[LogRecordFilter]) extends FilterPane {

  val initSev = of match {
    case Some(f: SeverityLogRecordFilter) => f.s
    case None => "UVM_INFO"
  }

  val label = new Label("Severity")

  val sevCombo = new ComboBox[String]() {
    items = ObservableBuffer("UVM_INFO", "UVM_WARNING", "UVM_ERROR", "UVM_FATAL")
    value = initSev
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

class TimeFilterPane(of: Option[LogRecordFilter]) extends FilterPane {

  val (initMin, initMax) = of match {
    case Some(f:TimeLogRecordFilter) => (f.min, f.max)
    case None => (0, BigDecimal(Double.MaxValue))
  }

  val label1 = new Label("Time inside")
  val minText = new TextField() {
    promptText = "min"
    text = initMin.toString()
  }
  val label2 = new Label(":")
  val maxText = new TextField() {
    promptText = "max"
    text = initMax.toString()
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
    val min: BigDecimal = minText.text() match {
      case "" => 0
      case n: String => BigDecimal(n)
    }
    val max: BigDecimal = maxText.text() match {
      case "" => BigDecimal(Double.MaxValue) // Arbitrary upper limit if the user does not bother to specify it
      case n: String => BigDecimal(n)
    }
    TimeLogRecordFilter(min, max)
  }
}

class HierFilterPane(of: Option[LogRecordFilter]) extends FilterPane {

  val initHier = of match {
    case Some(f: HierLogRecordFilter) => f.h
    case None => ""
  }

  val hierText = new TextField() {
    promptText = "hierarchy"
    text = initHier
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

class CompNameFilterPane(of: Option[LogRecordFilter]) extends FilterPane {

  val initComp = of match {
    case Some(f: CompNameLogRecordFilter) => f.c
    case None => ""
  }

  val compNameText = new TextField() {
    promptText = "Component"
    text = initComp
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

class TextContainsFilterPane(of: Option[LogRecordFilter]) extends FilterPane {

  val initText = of match {
    case Some(f: TextContainsLogRecordFilter) => f.s
    case None => ""
  }

  val textContainsText = new TextField() {
    promptText = "text"
    text = initText
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