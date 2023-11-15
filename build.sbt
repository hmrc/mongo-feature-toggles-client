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
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val libName = "mongo-feature-toggles-client"

val scala2_13 = "2.13.12"
val scala2_12 = "2.12.18"

lazy val library = Project(s"$libName-play-28", file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(IntegrationTest / unmanagedSourceDirectories  := (IntegrationTest / baseDirectory)(base => Seq(
    base / "src" / "it" / "scala"
  )).value)
  .settings(
    Compile / unmanagedSourceDirectories    += baseDirectory.value / "/src/main",
    Test    / unmanagedSourceDirectories    += baseDirectory.value / "/src/test",
    name := libName,
    scalaVersion := scala2_13,
    organization := "uk.gov.hmrc",
    crossScalaVersions := Seq(scala2_13, scala2_12),
    libraryDependencies ++= BuildDependencies(),
    isPublicArtefact := true,
    majorVersion     := 0,
    scalafmtOnCompile := true,
    scalacOptions ++= Seq(
      "-feature",
      "-Werror",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    ),
    resolvers += Resolver.typesafeRepo("releases"),
    Test / fork := true //Required to prevent https://github.com/sbt/sbt/issues/4609,
  )
  .settings(routesImport ++= Seq("uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName"))
  .settings(ScoverageSettings())
  .settings(libraryDependencySchemes ++= Seq(
    "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always))
