
//Imports
import sbt._

object Dependencies {

  //List of Versions
  val V = new {
    //Seed
    val seeds                       = "4.0.8"
    //Test
    val scalaTest                   = "3.0.5"
  }

  //List of Dependencies
  val D = new {
    val seeds                       = "zeab" %% "seeds" % V.seeds
    val scalaTest                   = "org.scalatest" %% "scalatest" % V.scalaTest % "test"
  }

  //Group Common Dependencies
  val commonDependencies: Seq[ModuleID] = Seq(
    D.seeds,
    D.scalaTest
  )

}

