resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

addSbtPlugin("com.typesafe.play" % "sbt-plugin"               % "2.8.20")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"            % "2.0.9")
addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"           % "3.15.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables"       % "2.2.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"             % "2.5.0")
