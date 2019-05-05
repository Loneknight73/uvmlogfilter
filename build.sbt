name := "uvmlogfilter"

version := "0.0.7"

scalaVersion := "2.12.8"

// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11"
// Add dependency on ScalaFX library
//libraryDependencies += "org.scalafx" %% "scalafx" % "12.0.1-R17"
//
//// Determine OS version of JavaFX binaries
//lazy val osName = System.getProperty("os.name") match {
//  case n if n.startsWith("Linux")   => "linux"
//  case n if n.startsWith("Mac")     => "mac"
//  case n if n.startsWith("Windows") => "win"
//  case _ => throw new Exception("Unknown platform!")
//}
//
//lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
//libraryDependencies ++= javaFXModules.map( m =>
//  "org.openjfx" % s"javafx-$m" % "12.0.1" classifier osName
//)

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-feature")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

mainClass in Compile := Some("com.github.uvmlogfilter.gui.UvmLogFilterGUI")