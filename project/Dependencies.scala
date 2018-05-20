
//Imports
import sbt._

object Dependencies {

  //List of Versions
  val V = new {
    val scalaTest                   = "3.0.5"
    val seeds                       = "4.0.7"
  }

  //List of Dependencies
  val D = new {
    val scalaTest                   = "org.scalatest" %% "scalatest" % V.scalaTest % "test"
    val seeds                       = "zeab" %% "seeds" % V.seeds
  }

  //Group Common Dependencies
  val commonDependencies: Seq[ModuleID] = Seq(
    D.seeds,
    D.scalaTest
  )

}

