name := "gatekeeper-client"

organization := "co.quine"

version := "0.0.9"

scalaVersion := "2.11.8"

isSnapshot := true

publishTo := Some("Quine snapshots" at "s3://snapshots.repo.quine.co")

resolvers ++= Seq[Resolver](
  "Quine Releases"                   at "s3://releases.repo.quine.co",
  "Quine Snapshots"                  at "s3://snapshots.repo.quine.co",
  "Typesafe repository snapshots"    at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases"     at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype repo"                    at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Sonatype releases"                at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots"               at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype staging"                 at "http://oss.sonatype.org/content/repositories/staging",
  "Sonatype release Repository"      at "http://oss.sonatype.org/service/local/staging/deploy/maven2/",
  "Java.net Maven2 Repository"       at "http://download.java.net/maven/2/"
)

lazy val versions = new {
  val akka = "2.4.3"
  val config = "1.3.0"
  val scalaj = "2.3.0"
}

libraryDependencies ++= Seq(
  "com.typesafe"                 % "config" % versions.config,
  "org.scalaj"                  %% "scalaj-http" % versions.scalaj,
  "com.typesafe.akka"           %% "akka-actor" % versions.akka,
  "com.typesafe.akka"           %% "akka-stream" % versions.akka
)