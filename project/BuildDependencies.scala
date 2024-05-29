import play.sbt.PlayImport.caffeine
import sbt.*

private object BuildDependencies {

  val hmrcMongoVersion          = "1.9.0"
  val bootstrapVersion          = "8.6.0"
  private val internalAuthClientVersion = "2.0.0"

  //Intentionally left on an older version
  val bootstrapVersion28 = "7.23.0"

  private val collectionCompatVersion   = "2.11.0"
  private val mockitoScalatestVersion   = "1.17.30"

  val playVersion28 = "play-28"
  val playVersion29 = "play-29"
  val playVersion30 = "play-30"

  val compile28: Seq[ModuleID] = Seq(
    caffeine,
    "uk.gov.hmrc"            %% s"bootstrap-frontend-$playVersion28"   % bootstrapVersion28,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVersion28"           % hmrcMongoVersion,
    "uk.gov.hmrc"            %% s"internal-auth-client-$playVersion28" % internalAuthClientVersion,
    "org.scala-lang.modules" %% "scala-collection-compat"              % collectionCompatVersion
  )

  val test28: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion28"  % bootstrapVersion28,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion28" % hmrcMongoVersion,
    "org.mockito"       %% "mockito-scala-scalatest"         % mockitoScalatestVersion
  ).map(_ % Test)

  val compile29: Seq[ModuleID] = Seq(
    caffeine,
    "uk.gov.hmrc"            %% s"bootstrap-frontend-$playVersion29"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVersion29"           % hmrcMongoVersion,
    "uk.gov.hmrc"            %% s"internal-auth-client-$playVersion29" % internalAuthClientVersion,
    "org.scala-lang.modules" %% "scala-collection-compat"              % collectionCompatVersion
  )

  val test29: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion29"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion29" % hmrcMongoVersion,
    "org.mockito"       %% "mockito-scala-scalatest"         % mockitoScalatestVersion
  ).map(_ % Test)

  val compile30: Seq[ModuleID] = Seq(
    caffeine,
    "uk.gov.hmrc"            %% s"bootstrap-frontend-$playVersion30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVersion30"           % hmrcMongoVersion,
    "uk.gov.hmrc"            %% s"internal-auth-client-$playVersion30" % internalAuthClientVersion,
    "org.scala-lang.modules" %% "scala-collection-compat"              % collectionCompatVersion
  )

  val test30: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion30" % hmrcMongoVersion,
    "org.mockito"       %% "mockito-scala-scalatest"         % mockitoScalatestVersion
  ).map(_ % Test)
}
