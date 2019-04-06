package UvmLogFilterGUI

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
