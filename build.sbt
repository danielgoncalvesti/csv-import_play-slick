import org.irundaia.sbt.sass._

name := """carteirinha-play-slick"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.2"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test


libraryDependencies ++= Seq(
  guice,
  filters,
  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B4",
  "org.webjars" % "bootstrap" % "4.0.0-alpha.6-1" exclude("org.webjars", "jquery"),
  "org.webjars" % "jquery" % "3.2.1",
  "org.webjars" % "font-awesome" % "4.7.0",
  "org.webjars" % "bootstrap-datepicker" % "1.4.0" exclude("org.webjars", "bootstrap"),
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
  "com.typesafe.play" %% "play-json" % "2.6.0",
  "com.h2database" % "h2" % "1.4.192",
  "com.novell.ldap" % "jldap" % "4.3", 
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "org.joda" % "joda-convert" % "1.7",
  "mysql" % "mysql-connector-java" % "5.1.38",
  "org.webjars" % "material-design-icons" % "3.0.1",
  "org.webjars.npm" % "materialize-css" % "0.99.0"
  )
  
SassKeys.cssStyle := Minified

SassKeys.generateSourceMaps := false

SassKeys.syntaxDetection := ForceScss
