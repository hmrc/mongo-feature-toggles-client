resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

sys.env.get("PLAY_VERSION") match {
  case Some("2.8") => // required since we're cross building for Play 2.8 which isn't compatible with sbt 1.9
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
  case _           => libraryDependencySchemes := libraryDependencySchemes.value // or any empty DslEntry
}

sys.env.get("PLAY_VERSION") match {
  case Some("2.8") => addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.21")
  case Some("2.9") => addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.2")
  case _           => addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.2")
}

addSbtPlugin("uk.gov.hmrc"   % "sbt-auto-build"     % "3.21.0")
addSbtPlugin("uk.gov.hmrc"   % "sbt-distributables" % "2.5.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt"       % "2.5.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage"      % "2.0.11")
