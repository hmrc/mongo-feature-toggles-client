import play.sbt.PlayImport.caffeine
import sbt.*

private object BuildDependencies {

  val hmrcMongoVersion          = "2.14.0"
  val bootstrapVersion          = "10.8.0"
  private val internalAuthClientVersion = "4.4.0"
  private val collectionCompatVersion   = "2.14.0"

  val playVersion30 = "play-30"

  val compile30: Seq[ModuleID] = Seq(
    caffeine,
    "uk.gov.hmrc"            %% s"bootstrap-frontend-$playVersion30"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVersion30"           % hmrcMongoVersion,
    "uk.gov.hmrc"            %% s"internal-auth-client-$playVersion30" % internalAuthClientVersion,
    "org.scala-lang.modules" %% "scala-collection-compat"              % collectionCompatVersion,
  )

  val test30: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion30" % hmrcMongoVersion,
  ).map(_ % Test)
}
