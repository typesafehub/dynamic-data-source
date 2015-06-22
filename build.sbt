import scalariform.formatter.preferences._
import bintray.Keys._

organization := "com.typesafe.dynamicdatasource"

name := "dynamic-data-source"

scalaVersion := "2.11.6"

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.8",
  "-encoding", "UTF-8"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

scalariformSettings
ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)

releaseSettings
ReleaseKeys.versionBump := sbtrelease.Version.Bump.Minor

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
publishMavenStyle := true
bintrayPublishSettings
repository in bintray := "maven-releases"
bintrayOrganization in bintray := Some("typesafe")