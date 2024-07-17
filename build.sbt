/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import BuildDependencies.{bootstrapVersion, hmrcMongoVersion, playVersion29, playVersion30}
import sbt.Keys.*
import sbt.*

val libName = "mongo-feature-toggles-client"

val scala2_13 = "2.13.12"
val scala2_12 = "2.12.18"

// Disable multiple project tests running at the same time, since notablescan flag is a global setting.
// https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html
Global / concurrentRestrictions += Tags.limitSum(1, Tags.Test, Tags.Untagged)

ThisBuild / scalaVersion       := scala2_13
ThisBuild / majorVersion       := 1
ThisBuild / isPublicArtefact   := true
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
ThisBuild / organization := "uk.gov.hmrc"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  "-Werror",
  "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
  "-Wconf:cat=unused&src=.*Routes\\.scala:s",
  "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
)

lazy val projects: Seq[ProjectReference] = sys.env.get("PLAY_VERSION") match {
  case Some("2.9") => Seq(play29, play29Test)
  case _ => Seq(play30, play30Test)
}

lazy val library = Project(libName, file("."))
  .settings(publish / skip := true)
  .aggregate(projects: _*)

def copyPlay30Sources(module: Project) =
  CopySources.copySources(
    module,
    transformSource   = _.replace("org.apache.pekko", "akka"),
    transformResource = _.replace("pekko", "akka")
  )

def copyPlay30Routes(module: Project) = Seq(
  Compile / routes / sources ++= {
    val dirs = (module / Compile / unmanagedResourceDirectories).value
    (dirs * "routes").get ++ (dirs * "*.routes").get
  }
)

lazy val play29 = Project(s"$libName-play-29", file(s"$libName-play-29"))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    routesImport ++= Seq("uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName"),
    ScoverageSettings(),
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= BuildDependencies.compile29,
    copyPlay30Sources(play30),
    copyPlay30Routes(play30)
  )

lazy val play29Test = Project(s"$libName-test-play-29", file(s"$libName-test-play-29"))
  .settings(libraryDependencies ++= BuildDependencies.test29)
  .dependsOn(play29)

lazy val play30 = Project(s"$libName-play-30", file(s"$libName-play-30"))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    routesImport ++= Seq("uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName"),
    ScoverageSettings(),
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= BuildDependencies.compile30,
    Compile / routes / sources ++= {
      val dirs = (Compile / unmanagedResourceDirectories).value
      (dirs * "routes").get ++ (dirs * "*.routes").get
    }
  )

lazy val play30Test = Project(s"$libName-test-play-30", file(s"$libName-test-play-30"))
  .settings(libraryDependencies ++= BuildDependencies.test30)
  .dependsOn(play30)

