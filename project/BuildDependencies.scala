import sbt._
import play.sbt.PlayImport._

private object BuildDependencies {
  private val playVersion = "play-28"
  private val hmrcMongoVersion = "1.3.0"
  private val bootstrapVersion = "7.23.0"

  val compile: Seq[ModuleID] = Seq(caffeine,
    "uk.gov.hmrc"            %% s"bootstrap-frontend-$playVersion"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-$playVersion"           % hmrcMongoVersion,
    "uk.gov.hmrc"            %% s"internal-auth-client-$playVersion" % "1.7.0",
    "org.scala-lang.modules" %% "scala-collection-compat"            % "2.11.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion"   % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion"  % hmrcMongoVersion,
    "org.mockito"       %% "mockito-scala-scalatest"        % "1.17.29"
  ).map(_ % "test, it")



  def apply(): Seq[ModuleID] = compile ++ test
}
