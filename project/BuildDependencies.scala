import play.sbt.PlayImport.caffeine
import sbt.*

private object BuildDependencies {

  val hmrcMongoVersion          = "2.1.0"
  val bootstrapVersion          = "9.0.0"
  private val internalAuthClientVersion = "3.0.0"
  private val collectionCompatVersion   = "2.12.0"
  private val mockitoScalatestVersion   = "1.17.37"

  val playVersion29 = "play-29"
  val playVersion30 = "play-30"

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
