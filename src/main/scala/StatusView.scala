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

import scalafx.scene.control.Label
import scalafx.scene.layout.VBox

class StatusView extends VBox {
  val logLabel = new Label("No log file opened")
  val filtLabel = new Label("No filter file opened")
  var nrec = 0
  var nfilt = 0
  val statsLabel = new Label(statsText(nrec, nfilt))
  this.spacing = 0
  this.children = List(logLabel, filtLabel, statsLabel)

  def setLog(s: String) = {
    logLabel.text = "Opened log file: " + s
  }

  def setFilter(s: String) = {
    filtLabel.text = "Opened filter file: " + s
  }

  def statsText(t: Int, f: Int) = {
    s"Total records: ${t}, filtered: ${f}"
  }

  def setTotal(n: Int) = {
    nrec = n
    statsLabel.text = statsText(nrec, nfilt)
  }

  def setFilt(n: Int) = {
    nfilt = n
    statsLabel.text = statsText(nrec, nfilt)
  }
}
