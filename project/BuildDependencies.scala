import play.sbt.PlayImport.caffeine
import sbt.*

private object BuildDependencies {
  private val playVersion28 = "play-28"
  private val hmrcMongoVersion = "1.5.0"
  private val bootstrapVersion28 = "7.23.0"

  private val playVersion29 = "play-29"
  private val bootstrapVersion29 = "8.0.0"

  private val playVersion30 = "play-30"
  private val bootstrapVersion30 = "8.0.0"

  val compile28: Seq[ModuleID] = Seq(caffeine,
    "uk.gov.hmrc"            %% s"bootstrap-frontend-$playVersion28"   % bootstrapVersion28,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVersion28"           % hmrcMongoVersion,
    "uk.gov.hmrc"            %% s"internal-auth-client-$playVersion28" % "1.7.0",
    "org.scala-lang.modules" %% "scala-collection-compat"            % "2.11.0"
  )

  val test28: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion28"   % bootstrapVersion28,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion28"  % hmrcMongoVersion,
    "org.mockito"       %% "mockito-scala-scalatest"        % "1.17.29"
  ).map(_ % "test")

  val compile29: Seq[ModuleID] = Seq(caffeine,
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion29" % bootstrapVersion29,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion29" % hmrcMongoVersion,
    "uk.gov.hmrc" %% s"internal-auth-client-$playVersion29" % "1.7.0",
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0"
  )

  val test29: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-test-$playVersion29" % bootstrapVersion29,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion29" % hmrcMongoVersion,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.29"
  ).map(_ % "test")

  val compile30: Seq[ModuleID] = Seq(caffeine,
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion30" % bootstrapVersion30,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion30" % hmrcMongoVersion,
    "uk.gov.hmrc" %% s"internal-auth-client-$playVersion30" % "1.8.0",
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0"
  )

  val test30: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-test-$playVersion30" % bootstrapVersion30,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion30" % hmrcMongoVersion,
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.29"
  ).map(_ % "test")


}
