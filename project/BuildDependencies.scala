import sbt._
import play.sbt.PlayImport._

private object BuildDependencies {
  private val playVersion = "play-28"
  private val hmrcMongoVersion = "1.1.0"

  val compile: Seq[ModuleID] = Seq(ehcache,
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion" % "7.13.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"         % hmrcMongoVersion,
    "uk.gov.hmrc"       %% s"internal-auth-client-$playVersion" % "1.2.0"
  )

  val test: Seq[ModuleID] = Seq(
      "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion
    ).map(_ % "test, it")



  def apply(): Seq[ModuleID] = compile ++ test
}
