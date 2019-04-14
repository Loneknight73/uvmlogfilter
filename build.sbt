name := "uvmlogfilter"

version := "0.0.5"

scalaVersion := "2.12.8"

// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.5"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-feature")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

mainClass in Compile := Some("UvmLogFilterGUI.UvmLogFilterGUI")