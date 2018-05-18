

//Imports
import Common._
import Dependencies._

//Disable the snapshot for root
publishArtifact := false

lazy val j2sJavaNet = (project in file("j2s-java-net"))
  .settings(baseProjectSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(commonResolvers: _*)
  .enablePlugins(Artifactory)

lazy val examples = (project in file("examples"))
  .settings(baseProjectSettings: _*)
  .dependsOn(j2sJavaNet)
  .settings(disablePublishing)