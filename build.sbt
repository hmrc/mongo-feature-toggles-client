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

import sbt.Keys._
import sbt._

val libName = "mongo-feature-toggles-client"

val scala2_13 = "2.13.12"
val scala2_12 = "2.12.18"

ThisBuild / scalaVersion       := scala2_13
ThisBuild / majorVersion       := 1
ThisBuild / isPublicArtefact   := true
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)

lazy val library = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .settings(
    sharedSources,
    name := libName,
    scalaVersion := scala2_13,
    organization := "uk.gov.hmrc",
    libraryDependencies ++= BuildDependencies.compile30 ++ BuildDependencies.test30,
    scalafmtOnCompile := true,
    scalacOptions ++= Seq(
      "-feature",
      "-Werror",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    )
  )
  .settings(routesImport ++= Seq("uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName"))
  .settings(ScoverageSettings())
  .settings(libraryDependencySchemes ++= Seq(
    "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always))
  .aggregate(
    play28,
    play29,
    play30
  )

lazy val play28 = Project(s"$libName-play-28", file("play-28"))
  .settings(
    crossScalaVersions := Seq(scala2_12, scala2_13),
    libraryDependencies ++= BuildDependencies.compile28 ++ BuildDependencies.test28,
    sharedSources
  )

lazy val play29 = Project(s"$libName-play-29", file("play-29"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= BuildDependencies.compile29 ++ BuildDependencies.test29,
    sharedSources
  )

lazy val play30 = Project(s"$libName-play-30", file("play-30"))
  .settings(
    crossScalaVersions := Seq(scala2_13),
    libraryDependencies ++= BuildDependencies.compile30 ++ BuildDependencies.test30,
    sharedSources
  )

def sharedSources = Seq(
  Compile / unmanagedSourceDirectories += baseDirectory.value / "../src/main/scala",
  Compile / unmanagedResourceDirectories += baseDirectory.value / "../src/main/resources",
  Test / unmanagedSourceDirectories += baseDirectory.value / "../src/test"
)
