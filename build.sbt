name := """phantom_crud"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"


libraryDependencies ++= Seq(
	"org.reactivemongo" %  "play2-reactivemongo_2.11" % "0.10.5.0.akka23",
	"org.reactivemongo" %  "reactivemongo-bson_2.11"  % "0.10.5.0.akka23",
	"com.google.apis" % "google-api-services-oauth2" % "v2-rev59-1.17.0-rc",
	"com.google.apis" % "google-api-services-drive" % "v2-rev162-1.19.1",
	"com.googlecode.json-simple" % "json-simple" % "1.1.1",
  jdbc,
  anorm,
  cache,
  ws
)