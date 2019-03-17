name := "uvmlogfilter"

version := "0.0.2"

scalaVersion := "2.12.8"

// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-feature")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

mainClass in Compile := Some("UvmLogFilterGUI.UvmLogFilterGUI")